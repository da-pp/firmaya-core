package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.firma.dto.AceptacionFirmaDto;
import com.firmaya.api.firma.dto.SolicitudAceptacionFirma;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-08, EP-52: aceptacion expresa, obligatoria antes de solicitar el OTP. */
@Service
@Transactional
public class ServicioAceptacionFirma {

    private static final String TEXTO_ACEPTACION =
            "He leido el contrato en su version congelada y acepto firmarlo electronicamente.";

    private final ServicioResolucionFirmante servicioResolucionFirmante;
    private final RepositorioAceptacionFirma repositorioAceptacionFirma;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;

    public ServicioAceptacionFirma(ServicioResolucionFirmante servicioResolucionFirmante,
                                    RepositorioAceptacionFirma repositorioAceptacionFirma,
                                    ServicioRegistroAuditoria servicioRegistroAuditoria) {
        this.servicioResolucionFirmante = servicioResolucionFirmante;
        this.repositorioAceptacionFirma = repositorioAceptacionFirma;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
    }

    public AceptacionFirmaDto registrarAceptacion(ContextoParticipanteAutenticado contexto,
                                                   SolicitudAceptacionFirma solicitud, String direccionIp) {
        ResolucionFirmante resolucion = servicioResolucionFirmante.resolver(contexto);
        var solicitudFirma = resolucion.solicitud();
        if (!solicitudFirma.getId().equals(solicitud.idSolicitud())) {
            throw new SolicitudInvalidaException("La solicitud indicada no corresponde a su acceso.");
        }
        if (!solicitudFirma.estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de firma ha vencido.");
        }
        if (!resolucion.proceso().getIdVersionObjetivo().equals(solicitud.idVersion())
                || !resolucion.proceso().getHashObjetivo().equalsIgnoreCase(solicitud.hashSha256().toLowerCase(Locale.ROOT))) {
            throw new ConflictoEstadoException("La version del contrato cambio; recargue antes de continuar.");
        }
        if (repositorioAceptacionFirma.findBySolicitudFirmaId(solicitudFirma.getId()).isPresent()) {
            throw new ConflictoEstadoException("Ya existe una aceptacion registrada para esta solicitud.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        AceptacionFirma aceptacion = AceptacionFirma.crear(UUID.randomUUID(), solicitudFirma, solicitud.idVersion(),
                resolucion.proceso().getHashObjetivo(), TEXTO_ACEPTACION, direccionIp, ahora);
        repositorioAceptacionFirma.save(aceptacion);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("ACEPTACION_FIRMA_REGISTRADA", "AceptacionFirma", "Aceptacion expresa registrada previa al OTP.")
                .conVersionContrato(resolucion.contrato().getId(), solicitud.idVersion())
                .conIp(direccionIp));

        return new AceptacionFirmaDto(aceptacion.getId(), solicitudFirma.getId(), solicitud.idVersion(),
                aceptacion.getFechaAceptacion(), "ACEPTADO");
    }
}
