package com.firmaya.api.notificaciones;

import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service interno transversal de envio de correo (no expuesto como endpoint), usado por
 * CU-21 (recuperacion) y disponible para el resto de eventos de CU-20. El fallo de entrega
 * nunca interrumpe el flujo de negocio: la Notificacion queda con estado_entrega = ERROR y
 * el llamador decide como continuar (en recuperacion, el mensaje generico igual se muestra).
 */
@Service
public class ServiceEnvioNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ServiceEnvioNotificaciones.class);

    private final JavaMailSender javaMailSender;
    private final RepositoryTipoEventoNotificacion repositoryTipoEventoNotificacion;
    private final RepositoryNotificacion repositoryNotificacion;
    private final String remitente;

    public ServiceEnvioNotificaciones(JavaMailSender javaMailSender,
                                        RepositoryTipoEventoNotificacion repositoryTipoEventoNotificacion,
                                        RepositoryNotificacion repositoryNotificacion,
                                        @Value("${firmaya.correo.remitente}") String remitente) {
        this.javaMailSender = javaMailSender;
        this.repositoryTipoEventoNotificacion = repositoryTipoEventoNotificacion;
        this.repositoryNotificacion = repositoryNotificacion;
        this.remitente = remitente;
    }

    /**
     * Envia un correo cuyo cuerpo NO contiene secretos (token, OTP, enlace tokenizado):
     * el mismo texto se persiste en firmaya.Notificacion.mensaje.
     */
    @Transactional
    public boolean enviarCorreoOperativo(Usuario destinatario, String codigoTipoEvento, String titulo, String mensaje) {
        return enviarCorreoOperativo(destinatario, codigoTipoEvento, titulo, mensaje, mensaje);
    }

    /**
     * Variante para correos que SI incluyen un secreto en el cuerpo enviado (por ejemplo, el
     * enlace de recuperacion de contrasena con su token). {@code cuerpoCorreo} se usa
     * exclusivamente para el envio y nunca se persiste; {@code resumenParaRegistro} (sin
     * secretos) es lo unico que queda en firmaya.Notificacion.mensaje, tal como exige la
     * politica de tokens del proyecto (nunca en texto claro en base de datos ni registros).
     */
    @Transactional
    public boolean enviarCorreoOperativo(Usuario destinatario, String codigoTipoEvento, String titulo,
                                          String cuerpoCorreo, String resumenParaRegistro) {
        TipoEventoNotificacion tipoEvento = repositoryTipoEventoNotificacion.findByCodigoAndActivoTrue(codigoTipoEvento)
                .orElseThrow(() -> new IllegalStateException(
                        "El tipo de evento de notificacion '" + codigoTipoEvento + "' no esta configurado."));

        Notificacion notificacion = Notificacion.deCorreo(UUID.randomUUID(), tipoEvento, destinatario,
                destinatario.getCorreoElectronico(), titulo, resumenParaRegistro, OffsetDateTime.now());
        // Se reasigna el resultado de save(): con @Id asignado manualmente, save() usa
        // merge() y devuelve una copia gestionada distinta de la original; enviarYRegistrar
        // debe mutar esa copia gestionada, no la instancia original ya desconectada.
        notificacion = repositoryNotificacion.save(notificacion);

        return enviarYRegistrar(notificacion, destinatario.getCorreoElectronico(), titulo, cuerpoCorreo, codigoTipoEvento);
    }

    /**
     * Envia un correo a un Participante (destinatario externo, no necesariamente con cuenta
     * interna), por ejemplo el enlace de invitacion o de solicitud de firma. Devuelve true si
     * el envio fue exitoso, para que el llamador actualice su propio estado (Invitacion,
     * SolicitudFirma) sin necesidad de volver a consultar la Notificacion persistida.
     */
    @Transactional
    public boolean enviarCorreoDirigidoAParticipante(UUID idContrato, UUID idParticipante, String correoDestino,
                                                       String codigoTipoEvento, String titulo, String cuerpoCorreo,
                                                       String resumenParaRegistro) {
        TipoEventoNotificacion tipoEvento = repositoryTipoEventoNotificacion.findByCodigoAndActivoTrue(codigoTipoEvento)
                .orElseThrow(() -> new IllegalStateException(
                        "El tipo de evento de notificacion '" + codigoTipoEvento + "' no esta configurado."));

        Notificacion notificacion = Notificacion.deCorreoParticipante(UUID.randomUUID(), tipoEvento, idContrato,
                idParticipante, correoDestino, titulo, resumenParaRegistro, OffsetDateTime.now());
        notificacion = repositoryNotificacion.save(notificacion);

        return enviarYRegistrar(notificacion, correoDestino, titulo, cuerpoCorreo, codigoTipoEvento);
    }

    private boolean enviarYRegistrar(Notificacion notificacion, String correoDestino, String titulo,
                                      String cuerpoCorreo, String codigoTipoEvento) {
        try {
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setFrom(remitente);
            correo.setTo(correoDestino);
            correo.setSubject(titulo);
            correo.setText(cuerpoCorreo);
            javaMailSender.send(correo);
            notificacion.marcarEnviada(OffsetDateTime.now());
            return true;
        } catch (MailException ex) {
            log.warn("No se pudo enviar el correo '{}' a un destinatario: {}", codigoTipoEvento, ex.getMessage());
            notificacion.marcarError(OffsetDateTime.now());
            return false;
        }
    }
}
