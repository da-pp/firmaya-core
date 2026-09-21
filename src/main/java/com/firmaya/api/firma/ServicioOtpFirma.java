package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.firma.dto.DesafioOtpDto;
import com.firmaya.api.firma.dto.SolicitudEmitirOtp;
import com.firmaya.api.firma.dto.SolicitudReenviarOtp;
import com.firmaya.api.notificaciones.ServicioEnvioNotificaciones;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-08, EP-53 y EP-54: emision y reenvio del codigo OTP, siempre posterior a la aceptacion expresa. */
@Service
@Transactional
public class ServicioOtpFirma {

    private final ServicioResolucionFirmante servicioResolucionFirmante;
    private final RepositorioAceptacionFirma repositorioAceptacionFirma;
    private final RepositorioDesafioOtp repositorioDesafioOtp;
    private final ServicioVerificadorOtp servicioVerificadorOtp;
    private final ServicioEnvioNotificaciones servicioEnvioNotificaciones;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final int otpMinutos;

    public ServicioOtpFirma(ServicioResolucionFirmante servicioResolucionFirmante,
                             RepositorioAceptacionFirma repositorioAceptacionFirma,
                             RepositorioDesafioOtp repositorioDesafioOtp,
                             ServicioVerificadorOtp servicioVerificadorOtp,
                             ServicioEnvioNotificaciones servicioEnvioNotificaciones,
                             ServicioRegistroAuditoria servicioRegistroAuditoria,
                             @Value("${firmaya.firma.otp-minutos}") int otpMinutos) {
        this.servicioResolucionFirmante = servicioResolucionFirmante;
        this.repositorioAceptacionFirma = repositorioAceptacionFirma;
        this.repositorioDesafioOtp = repositorioDesafioOtp;
        this.servicioVerificadorOtp = servicioVerificadorOtp;
        this.servicioEnvioNotificaciones = servicioEnvioNotificaciones;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.otpMinutos = otpMinutos;
    }

    public DesafioOtpDto emitir(ContextoParticipanteAutenticado contexto, SolicitudEmitirOtp solicitud) {
        ResolucionFirmante resolucion = servicioResolucionFirmante.resolver(contexto);
        validarSolicitudCorresponde(resolucion, solicitud.idSolicitud());
        AceptacionFirma aceptacion = repositorioAceptacionFirma.findBySolicitudFirmaId(solicitud.idSolicitud())
                .orElseThrow(() -> new ConflictoEstadoException("Debe registrar la aceptacion expresa antes de solicitar el codigo."));
        if (!aceptacion.getId().equals(solicitud.idAceptacion())) {
            throw new SolicitudInvalidaException("La aceptacion indicada no corresponde a esta solicitud.");
        }

        repositorioDesafioOtp.findByIdSolicitudFirmaAndActivoTrue(solicitud.idSolicitud())
                .ifPresent(previo -> previo.invalidar(OffsetDateTime.now()));

        return generarYEnviarOtp(resolucion, aceptacion);
    }

    public DesafioOtpDto reenviar(ContextoParticipanteAutenticado contexto, SolicitudReenviarOtp solicitud) {
        ResolucionFirmante resolucion = servicioResolucionFirmante.resolver(contexto);
        validarSolicitudCorresponde(resolucion, solicitud.idSolicitud());

        DesafioOtp desafioAnterior = repositorioDesafioOtp
                .findByIdAndIdSolicitudFirma(solicitud.idDesafio(), solicitud.idSolicitud())
                .orElseThrow(() -> new RecursoNoEncontradoException("El desafio OTP no existe."));
        if (desafioAnterior.estaBloqueado()) {
            throw new ConflictoEstadoException("El enlace de firma ha sido bloqueado por seguridad. Contacte al dueno del contrato.");
        }
        AceptacionFirma aceptacion = repositorioAceptacionFirma.findBySolicitudFirmaId(solicitud.idSolicitud())
                .orElseThrow(() -> new ConflictoEstadoException("Debe registrar la aceptacion expresa antes de solicitar el codigo."));

        OffsetDateTime ahora = OffsetDateTime.now();
        if (desafioAnterior.isActivo()) {
            desafioAnterior.invalidar(ahora);
        }
        return generarYEnviarOtp(resolucion, aceptacion);
    }

    private DesafioOtpDto generarYEnviarOtp(ResolucionFirmante resolucion, AceptacionFirma aceptacion) {
        if (!resolucion.solicitud().estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de firma ha vencido.");
        }
        String otp = servicioVerificadorOtp.generarOtp();
        byte[] verificador = servicioVerificadorOtp.calcularVerificador(otp, resolucion.solicitud().getId());

        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime expiracion = ahora.plusMinutes(otpMinutos);
        DesafioOtp desafio = DesafioOtp.crear(UUID.randomUUID(), resolucion.solicitud().getId(), aceptacion,
                verificador, ahora, expiracion);
        repositorioDesafioOtp.save(desafio);

        String correo = resolucion.firmante().getCorreoCongelado();
        String cuerpoCorreo = "Tu codigo de verificacion para firmar el contrato \"" + resolucion.contrato().getNombre()
                + "\" en FirmaYA es: " + otp + ". Vence en " + otpMinutos + " minutos. No lo compartas con nadie.";
        String resumen = "Se envio un codigo de verificacion (OTP) para firmar, valido " + otpMinutos + " minutos.";
        servicioEnvioNotificaciones.enviarCorreoDirigidoAParticipante(resolucion.contrato().getId(),
                resolucion.firmante().getParticipante().getId(), correo, "CONTRATO_SOLICITUD_FIRMA",
                "Codigo de verificacion - FirmaYA", cuerpoCorreo, resumen);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("OTP_EMITIDO", "DesafioOtp", "Codigo OTP emitido para firma.")
                .conEntidad(desafio.getId())
                .conContrato(resolucion.contrato().getId()));

        return new DesafioOtpDto(desafio.getId(), enmascarar(correo), expiracion, 3);
    }

    private void validarSolicitudCorresponde(ResolucionFirmante resolucion, UUID idSolicitud) {
        if (!resolucion.solicitud().getId().equals(idSolicitud)) {
            throw new SolicitudInvalidaException("La solicitud indicada no corresponde a su acceso.");
        }
        if (resolucion.solicitud().getFirmanteProceso() == null) {
            throw new RecursoNoEncontradoException("La solicitud de firma no existe.");
        }
    }

    private String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 1) {
            return correo;
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
