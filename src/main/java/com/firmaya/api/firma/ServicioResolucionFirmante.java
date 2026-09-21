package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.ProcesoFirma;
import com.firmaya.api.procesofirma.RepositorioFirmanteProceso;
import com.firmaya.api.procesofirma.RepositorioProcesoFirma;
import com.firmaya.api.procesofirma.RepositorioSolicitudFirma;
import com.firmaya.api.procesofirma.SolicitudFirma;
import org.springframework.stereotype.Component;

/**
 * Resuelve, a partir de una sesion externa de proposito FIRMA, el proceso/firmante/solicitud
 * vigentes del participante autenticado. Compartido por EP-51..EP-55 (CU-08).
 */
@Component
class ServicioResolucionFirmante {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioProcesoFirma repositorioProcesoFirma;
    private final RepositorioFirmanteProceso repositorioFirmanteProceso;
    private final RepositorioSolicitudFirma repositorioSolicitudFirma;

    ServicioResolucionFirmante(RepositorioContrato repositorioContrato,
                                RepositorioProcesoFirma repositorioProcesoFirma,
                                RepositorioFirmanteProceso repositorioFirmanteProceso,
                                RepositorioSolicitudFirma repositorioSolicitudFirma) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioProcesoFirma = repositorioProcesoFirma;
        this.repositorioFirmanteProceso = repositorioFirmanteProceso;
        this.repositorioSolicitudFirma = repositorioSolicitudFirma;
    }

    ResolucionFirmante resolver(ContextoParticipanteAutenticado contexto) {
        if (contexto.proposito() != PropositoAcceso.FIRMA) {
            throw new AccesoDenegadoNegocioException("Este acceso no habilita operaciones de firma.");
        }
        Contrato contrato = repositorioContrato.findById(contexto.idContrato())
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        if (contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("El contrato ya no esta Listo para firmar.");
        }
        ProcesoFirma proceso = repositorioProcesoFirma.findByContratoIdAndFinalizadoFalse(contrato.getId())
                .orElseThrow(() -> new ConflictoEstadoException("No hay un proceso de firma activo."));
        FirmanteProceso firmante = repositorioFirmanteProceso
                .findByProcesoFirmaIdAndParticipanteId(proceso.getId(), contexto.idParticipante())
                .orElseThrow(() -> new RecursoNoEncontradoException("No es firmante en el proceso vigente."));
        SolicitudFirma solicitud = repositorioSolicitudFirma.findByFirmanteProcesoId(firmante.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una solicitud de firma asociada."));
        return new ResolucionFirmante(contrato, proceso, firmante, solicitud);
    }
}
