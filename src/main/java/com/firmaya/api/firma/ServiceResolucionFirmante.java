package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.ProcesoFirma;
import com.firmaya.api.procesofirma.RepositoryFirmanteProceso;
import com.firmaya.api.procesofirma.RepositoryProcesoFirma;
import com.firmaya.api.procesofirma.RepositorySolicitudFirma;
import com.firmaya.api.procesofirma.SolicitudFirma;
import org.springframework.stereotype.Component;

/**
 * Resuelve, a partir de una sesion externa de proposito FIRMA, el proceso/firmante/solicitud
 * vigentes del participante autenticado. Compartido por EP-51..EP-55 (CU-08).
 */
@Component
class ServiceResolucionFirmante {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositorySolicitudFirma repositorySolicitudFirma;

    ServiceResolucionFirmante(RepositoryContrato repositoryContrato,
                                RepositoryProcesoFirma repositoryProcesoFirma,
                                RepositoryFirmanteProceso repositoryFirmanteProceso,
                                RepositorySolicitudFirma repositorySolicitudFirma) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositorySolicitudFirma = repositorySolicitudFirma;
    }

    ResolucionFirmante resolver(ContextoParticipanteAutenticado contexto) {
        if (contexto.proposito() != PropositoAcceso.FIRMA) {
            throw new AccesoDenegadoNegocioException("Este acceso no habilita operaciones de firma.");
        }
        Contrato contrato = repositoryContrato.findById(contexto.idContrato())
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        if (contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("El contrato ya no esta Listo para firmar.");
        }
        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(contrato.getId())
                .orElseThrow(() -> new ConflictoEstadoException("No hay un proceso de firma activo."));
        FirmanteProceso firmante = repositoryFirmanteProceso
                .findByProcesoFirmaIdAndParticipanteId(proceso.getId(), contexto.idParticipante())
                .orElseThrow(() -> new RecursoNoEncontradoException("No es firmante en el proceso vigente."));
        SolicitudFirma solicitud = repositorySolicitudFirma.findByFirmanteProcesoId(firmante.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una solicitud de firma asociada."));
        return new ResolucionFirmante(contrato, proceso, firmante, solicitud);
    }
}
