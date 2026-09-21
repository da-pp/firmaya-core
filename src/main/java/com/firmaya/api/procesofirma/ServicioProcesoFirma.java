package com.firmaya.api.procesofirma;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.HistorialEstadoContrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.RepositorioHistorialEstadoContrato;
import com.firmaya.api.contratos.ServicioAutorizacionContrato;
import com.firmaya.api.firma.RepositorioFirma;
import com.firmaya.api.procesofirma.dto.ResumenProcesoFirmaDto;
import com.firmaya.api.procesofirma.dto.SolicitudCancelarProcesoFirma;
import com.firmaya.api.tokens.RepositorioTokenAcceso;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-05, EP-45: cancelar el proceso de firma antes de la primera firma valida. */
@Service
@Transactional
public class ServicioProcesoFirma {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioProcesoFirma repositorioProcesoFirma;
    private final RepositorioSolicitudFirma repositorioSolicitudFirma;
    private final RepositorioFirma repositorioFirma;
    private final RepositorioTokenAcceso repositorioTokenAcceso;
    private final RepositorioHistorialEstadoContrato repositorioHistorialEstadoContrato;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;

    public ServicioProcesoFirma(RepositorioContrato repositorioContrato,
                                 RepositorioProcesoFirma repositorioProcesoFirma,
                                 RepositorioSolicitudFirma repositorioSolicitudFirma,
                                 RepositorioFirma repositorioFirma,
                                 RepositorioTokenAcceso repositorioTokenAcceso,
                                 RepositorioHistorialEstadoContrato repositorioHistorialEstadoContrato,
                                 RepositorioUsuario repositorioUsuario,
                                 ServicioAutorizacionContrato servicioAutorizacionContrato,
                                 ServicioRegistroAuditoria servicioRegistroAuditoria) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioProcesoFirma = repositorioProcesoFirma;
        this.repositorioSolicitudFirma = repositorioSolicitudFirma;
        this.repositorioFirma = repositorioFirma;
        this.repositorioTokenAcceso = repositorioTokenAcceso;
        this.repositorioHistorialEstadoContrato = repositorioHistorialEstadoContrato;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
    }

    public ResumenProcesoFirmaDto cancelarAntesPrimeraFirma(UUID idContrato, UUID idUsuario,
                                                             SolicitudCancelarProcesoFirma solicitud) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        if (contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("Solo se puede cancelar un contrato Listo para firmar.");
        }
        ProcesoFirma proceso = repositorioProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));

        long firmasRegistradas = repositorioFirma.countByProcesoFirmaId(proceso.getId());
        if (firmasRegistradas > 0) {
            throw new ConflictoEstadoException(
                    "No es posible cancelar: ya existe al menos una firma registrada en este proceso.");
        }

        Usuario actor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        OffsetDateTime ahora = OffsetDateTime.now();

        List<SolicitudFirma> solicitudes = repositorioSolicitudFirma.findByProcesoFirmaId(proceso.getId());
        for (SolicitudFirma solicitudFirma : solicitudes) {
            solicitudFirma.revocar();
            repositorioTokenAcceso.findByIdSolicitudFirmaAndFechaConsumoIsNullAndFechaRevocacionIsNull(solicitudFirma.getId())
                    .forEach(token -> token.revocar(ahora, "Proceso de firma cancelado antes de la primera firma."));
        }

        proceso.cancelar(actor, solicitud.motivo(), ahora);

        EstadoContrato anterior = contrato.getEstado();
        contrato.cambiarEstado(EstadoContrato.EN_REVISION, ahora);
        repositorioHistorialEstadoContrato.save(HistorialEstadoContrato.crear(UUID.randomUUID(), contrato, anterior,
                EstadoContrato.EN_REVISION, actor, proceso.getId(), solicitud.motivo(), ahora));

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(actor, "PROCESO_FIRMA_CANCELADO", "ProcesoFirma",
                        "Proceso de firma cancelado antes de la primera firma; contrato vuelve a En Revision.")
                .conEntidad(proceso.getId())
                .conContrato(idContrato));

        return new ResumenProcesoFirmaDto(proceso.getId(), idContrato, proceso.getEstado(),
                proceso.getFechaCancelacion(), contrato.getEstado());
    }
}
