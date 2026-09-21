package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.contratos.RepositorioVersionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.firma.dto.ContextoFirmaDto;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-08, EP-51: contenido congelado que el firmante debe leer antes de aceptar y firmar. */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaFirmante {

    private final ServicioResolucionFirmante servicioResolucionFirmante;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final RepositorioFirma repositorioFirma;

    public ServicioConsultaFirmante(ServicioResolucionFirmante servicioResolucionFirmante,
                                     RepositorioVersionContrato repositorioVersionContrato,
                                     RepositorioFirma repositorioFirma) {
        this.servicioResolucionFirmante = servicioResolucionFirmante;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.repositorioFirma = repositorioFirma;
    }

    public ContextoFirmaDto obtenerContexto(ContextoParticipanteAutenticado contexto) {
        ResolucionFirmante resolucion = servicioResolucionFirmante.resolver(contexto);
        var solicitud = resolucion.solicitud();
        if (!solicitud.estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de firma ha vencido.");
        }

        VersionContrato version = repositorioVersionContrato
                .findByIdAndContratoId(resolucion.proceso().getIdVersionObjetivo(), resolucion.contrato().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La version congelada no existe."));
        boolean yaFirmado = repositorioFirma.findByFirmanteProcesoId(resolucion.firmante().getId()).isPresent();

        return new ContextoFirmaDto(solicitud.getId(), resolucion.contrato().getId(), version.getId(),
                version.getNumeroVersion(), version.getHashSha256(), resolucion.contrato().getNombre(),
                version.getContenido(), solicitud.getFechaLimite(), !yaFirmado, yaFirmado);
    }
}
