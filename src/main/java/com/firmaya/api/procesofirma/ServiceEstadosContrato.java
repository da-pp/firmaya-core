package com.firmaya.api.procesofirma;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.HistorialEstadoContrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.RepositoryHistorialEstadoContrato;
import com.firmaya.api.contratos.RepositoryVersionContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.participantes.Participante;
import com.firmaya.api.participantes.RepositoryParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import com.firmaya.api.procesofirma.dto.CambioEstadoContratoDto;
import com.firmaya.api.procesofirma.dto.OpcionTransicionDto;
import com.firmaya.api.procesofirma.dto.OpcionesTransicionDto;
import com.firmaya.api.procesofirma.dto.SolicitudCambiarEstadoContrato;
import com.firmaya.api.usuarios.RepositoryUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-05, EP-43 y EP-44: transiciones manuales del contrato. BORRADOR-&gt;EN_REVISION,
 * EN_REVISION-&gt;LISTO_PARA_FIRMAR (congela version/hash/firmantes en ProcesoFirma) y
 * FIRMADO-&gt;ARCHIVADO. La transicion LISTO_PARA_FIRMAR-&gt;EN_REVISION solo ocurre via
 * cancelacion (EP-45, ServiceProcesoFirma) y FIRMADO es exclusivamente automatico (CU-08).
 */
@Service
@Transactional
public class ServiceEstadosContrato {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final RepositoryParticipante repositoryParticipante;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato;
    private final RepositoryUsuario repositoryUsuario;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceEstadosContrato(RepositoryContrato repositoryContrato,
                                    RepositoryVersionContrato repositoryVersionContrato,
                                    RepositoryParticipante repositoryParticipante,
                                    RepositoryProcesoFirma repositoryProcesoFirma,
                                    RepositoryFirmanteProceso repositoryFirmanteProceso,
                                    RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato,
                                    RepositoryUsuario repositoryUsuario,
                                    ServiceAutorizacionContrato serviceAutorizacionContrato,
                                    ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.repositoryParticipante = repositoryParticipante;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositoryHistorialEstadoContrato = repositoryHistorialEstadoContrato;
        this.repositoryUsuario = repositoryUsuario;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    @Transactional(readOnly = true)
    public OpcionesTransicionDto obtenerTransicionesManuales(UUID idContrato, UUID idUsuario) {
        Contrato contrato = obtenerContrato(idContrato);
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        List<OpcionTransicionDto> permitidas = new ArrayList<>();
        boolean hayFirmantes = !repositoryParticipante
                .findByContratoIdAndRolParticipacion(idContrato, RolParticipacion.FIRMANTE).isEmpty();

        switch (contrato.getEstado()) {
            case BORRADOR -> permitidas.add(new OpcionTransicionDto(EstadoContrato.EN_REVISION,
                    "Envia el contrato a revision.", true, null));
            case EN_REVISION -> permitidas.add(new OpcionTransicionDto(EstadoContrato.LISTO_PARA_FIRMAR,
                    "Congela la version actual y la lista de firmantes; habilita el envio de solicitudes de firma.",
                    hayFirmantes, hayFirmantes ? null : "Debe asignar al menos un firmante."));
            case FIRMADO -> permitidas.add(new OpcionTransicionDto(EstadoContrato.ARCHIVADO,
                    "Archiva el contrato firmado.", true, null));
            default -> {
            }
        }
        return new OpcionesTransicionDto(contrato.getEstado(), permitidas);
    }

    public CambioEstadoContratoDto cambiarEstado(UUID idContrato, UUID idUsuario, SolicitudCambiarEstadoContrato solicitud) {
        Contrato contrato = obtenerContrato(idContrato);
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        if (contrato.getEstado() != solicitud.estadoActualEsperado()) {
            throw new ConflictoEstadoException("El estado del contrato cambio; recargue antes de continuar.");
        }
        validarTransicion(contrato.getEstado(), solicitud.estadoDestino());

        Usuario actor = repositoryUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        EstadoContrato anterior = contrato.getEstado();
        OffsetDateTime ahora = OffsetDateTime.now();
        UUID idProcesoFirma = null;

        if (solicitud.estadoDestino() == EstadoContrato.LISTO_PARA_FIRMAR) {
            idProcesoFirma = congelarProcesoFirma(contrato, ahora);
        }

        contrato.cambiarEstado(solicitud.estadoDestino(), ahora);

        repositoryHistorialEstadoContrato.save(HistorialEstadoContrato.crear(UUID.randomUUID(), contrato, anterior,
                solicitud.estadoDestino(), actor, idProcesoFirma, solicitud.motivo(), ahora));

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(actor, "CONTRATO_CAMBIO_ESTADO", "Contrato",
                        "Transicion de " + anterior + " a " + solicitud.estadoDestino() + ".")
                .conContrato(idContrato));

        return new CambioEstadoContratoDto(idContrato, anterior, solicitud.estadoDestino(), ahora,
                contrato.getIdVersionActual(), idProcesoFirma);
    }

    private void validarTransicion(EstadoContrato actual, EstadoContrato destino) {
        boolean valida = (actual == EstadoContrato.BORRADOR && destino == EstadoContrato.EN_REVISION)
                || (actual == EstadoContrato.EN_REVISION && destino == EstadoContrato.LISTO_PARA_FIRMAR)
                || (actual == EstadoContrato.FIRMADO && destino == EstadoContrato.ARCHIVADO);
        if (!valida) {
            throw new ConflictoEstadoException("Esta transicion de estado no es posible. Verifique el flujo permitido.");
        }
    }

    private UUID congelarProcesoFirma(Contrato contrato, OffsetDateTime ahora) {
        List<Participante> firmantes = repositoryParticipante
                .findByContratoIdAndRolParticipacion(contrato.getId(), RolParticipacion.FIRMANTE);
        if (firmantes.isEmpty()) {
            throw new ConflictoEstadoException("Debe asignar al menos un firmante.");
        }
        verificarVersionActualNoNula(contrato);
        var versionActual = repositoryVersionContrato.findByIdAndContratoId(contrato.getIdVersionActual(), contrato.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La version actual no existe."));

        ProcesoFirma proceso = ProcesoFirma.crear(UUID.randomUUID(), contrato, versionActual.getId(),
                versionActual.getHashSha256(), ahora);
        proceso.activar();
        repositoryProcesoFirma.save(proceso);

        for (Participante firmante : firmantes) {
            repositoryFirmanteProceso.save(FirmanteProceso.crear(UUID.randomUUID(), proceso, firmante, ahora));
        }
        return proceso.getId();
    }

    private void verificarVersionActualNoNula(Contrato contrato) {
        if (contrato.getIdVersionActual() == null) {
            throw new ConflictoEstadoException("El contrato no tiene una version para congelar.");
        }
    }

    private Contrato obtenerContrato(UUID idContrato) {
        return repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
    }
}
