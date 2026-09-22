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
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.firma.RepositoryFirma;
import com.firmaya.api.procesofirma.dto.ResumenProcesoFirmaDto;
import com.firmaya.api.procesofirma.dto.SolicitudCancelarProcesoFirma;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.usuarios.RepositoryUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-05, EP-45: cancelar el proceso de firma antes de la primera firma valida. */
@Service
@Transactional
public class ServiceProcesoFirma {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositorySolicitudFirma repositorySolicitudFirma;
    private final RepositoryFirma repositoryFirma;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato;
    private final RepositoryUsuario repositoryUsuario;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceProcesoFirma(RepositoryContrato repositoryContrato,
                                 RepositoryProcesoFirma repositoryProcesoFirma,
                                 RepositorySolicitudFirma repositorySolicitudFirma,
                                 RepositoryFirma repositoryFirma,
                                 RepositoryTokenAcceso repositoryTokenAcceso,
                                 RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato,
                                 RepositoryUsuario repositoryUsuario,
                                 ServiceAutorizacionContrato serviceAutorizacionContrato,
                                 ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositorySolicitudFirma = repositorySolicitudFirma;
        this.repositoryFirma = repositoryFirma;
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.repositoryHistorialEstadoContrato = repositoryHistorialEstadoContrato;
        this.repositoryUsuario = repositoryUsuario;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    public ResumenProcesoFirmaDto cancelarAntesPrimeraFirma(UUID idContrato, UUID idUsuario,
                                                             SolicitudCancelarProcesoFirma solicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        if (contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("Solo se puede cancelar un contrato Listo para firmar.");
        }
        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));

        long firmasRegistradas = repositoryFirma.countByProcesoFirmaId(proceso.getId());
        if (firmasRegistradas > 0) {
            throw new ConflictoEstadoException(
                    "No es posible cancelar: ya existe al menos una firma registrada en este proceso.");
        }

        Usuario actor = repositoryUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        OffsetDateTime ahora = OffsetDateTime.now();

        List<SolicitudFirma> solicitudes = repositorySolicitudFirma.findByProcesoFirmaId(proceso.getId());
        for (SolicitudFirma solicitudFirma : solicitudes) {
            solicitudFirma.revocar();
            repositoryTokenAcceso.findByIdSolicitudFirmaAndFechaConsumoIsNullAndFechaRevocacionIsNull(solicitudFirma.getId())
                    .forEach(token -> token.revocar(ahora, "Proceso de firma cancelado antes de la primera firma."));
        }

        proceso.cancelar(actor, solicitud.motivo(), ahora);

        EstadoContrato anterior = contrato.getEstado();
        contrato.cambiarEstado(EstadoContrato.EN_REVISION, ahora);
        repositoryHistorialEstadoContrato.save(HistorialEstadoContrato.crear(UUID.randomUUID(), contrato, anterior,
                EstadoContrato.EN_REVISION, actor, proceso.getId(), solicitud.motivo(), ahora));

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(actor, "PROCESO_FIRMA_CANCELADO", "ProcesoFirma",
                        "Proceso de firma cancelado antes de la primera firma; contrato vuelve a En Revision.")
                .conEntidad(proceso.getId())
                .conContrato(idContrato));

        return new ResumenProcesoFirmaDto(proceso.getId(), idContrato, proceso.getEstado(),
                proceso.getFechaCancelacion(), contrato.getEstado());
    }
}
