package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.contratos.RepositoryVersionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.firma.dto.ContextoFirmaDto;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-08, EP-51: contenido congelado que el firmante debe leer antes de aceptar y firmar. */
@Service
@Transactional(readOnly = true)
public class ServiceConsultaFirmante {

    private final ServiceResolucionFirmante serviceResolucionFirmante;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final RepositoryFirma repositoryFirma;

    public ServiceConsultaFirmante(ServiceResolucionFirmante serviceResolucionFirmante,
                                     RepositoryVersionContrato repositoryVersionContrato,
                                     RepositoryFirma repositoryFirma) {
        this.serviceResolucionFirmante = serviceResolucionFirmante;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.repositoryFirma = repositoryFirma;
    }

    public ContextoFirmaDto obtenerContexto(ContextoParticipanteAutenticado contexto) {
        ResolucionFirmante resolucion = serviceResolucionFirmante.resolver(contexto);
        var solicitud = resolucion.solicitud();
        if (!solicitud.estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de firma ha vencido.");
        }

        VersionContrato version = repositoryVersionContrato
                .findByIdAndContratoId(resolucion.proceso().getIdVersionObjetivo(), resolucion.contrato().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La version congelada no existe."));
        boolean yaFirmado = repositoryFirma.findByFirmanteProcesoId(resolucion.firmante().getId()).isPresent();

        return new ContextoFirmaDto(solicitud.getId(), resolucion.contrato().getId(), version.getId(),
                version.getNumeroVersion(), version.getHashSha256(), resolucion.contrato().getNombre(),
                version.getContenido(), solicitud.getFechaLimite(), !yaFirmado, yaFirmado);
    }
}
