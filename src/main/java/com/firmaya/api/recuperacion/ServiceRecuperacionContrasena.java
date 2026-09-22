package com.firmaya.api.recuperacion;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.MensajeDto;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.comun.excepciones.TokenInvalidoException;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.recuperacion.dto.SolicitudCompletarRecuperacion;
import com.firmaya.api.recuperacion.dto.ValidacionRecuperacionDto;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.PropositoToken;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.EstadoActivacion;
import com.firmaya.api.usuarios.EstadoAdministrativo;
import com.firmaya.api.usuarios.RepositoryUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21, EP-06..EP-08. La respuesta de solicitud es SIEMPRE el mismo mensaje generico,
 * exista o no la cuenta, para no revelar informacion sensible.
 *
 * Elegibilidad de cuentas: solo se emite/consume el token para cuentas con
 * estado_administrativo = ACTIVO y estado_activacion = COMPLETADA (ya tienen una
 * contrasena que reemplazar). El tratamiento de cuentas inactivas o pendientes sigue
 * [PENDIENTE] en la especificacion (PD-03); esta es una decision tecnica provisional,
 * documentada para revision.
 */
@Service
@Transactional
public class ServiceRecuperacionContrasena {

    private static final String MENSAJE_GENERICO =
            "Si el correo electronico ingresado corresponde a una cuenta registrada, "
                    + "recibiras instrucciones para restablecer tu contrasena.";

    private final RepositoryUsuario repositoryUsuario;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final PasswordEncoder passwordEncoder;
    private final int duracionMinutos;
    private final String urlRecuperacion;

    public ServiceRecuperacionContrasena(RepositoryUsuario repositoryUsuario,
                                           RepositoryTokenAcceso repositoryTokenAcceso,
                                           ServiceHashCredencial serviceHashCredencial,
                                           ServiceEnvioNotificaciones serviceEnvioNotificaciones,
                                           ServiceRegistroAuditoria serviceRegistroAuditoria,
                                           PasswordEncoder passwordEncoder,
                                           @Value("${firmaya.recuperacion.duracion-minutos}") int duracionMinutos,
                                           @Value("${firmaya.frontend.url-recuperacion-contrasena}") String urlRecuperacion) {
        this.repositoryUsuario = repositoryUsuario;
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.passwordEncoder = passwordEncoder;
        this.duracionMinutos = duracionMinutos;
        this.urlRecuperacion = urlRecuperacion;
    }

    public MensajeDto solicitarRecuperacion(String correoElectronico) {
        String correoNormalizado = correoElectronico.trim().toLowerCase(Locale.ROOT);
        repositoryUsuario.findByCorreoNormalizado(correoNormalizado)
                .filter(this::esElegibleParaRecuperacion)
                .ifPresent(this::emitirTokenYNotificar);
        return new MensajeDto(MENSAJE_GENERICO);
    }

    @Transactional(readOnly = true)
    public ValidacionRecuperacionDto validarToken(String tokenPlano) {
        TokenAcceso token = buscarTokenVigenteOLanzar(tokenPlano);
        return new ValidacionRecuperacionDto(true, token.getFechaExpiracion());
    }

    public MensajeDto completarRecuperacion(SolicitudCompletarRecuperacion solicitud) {
        if (!solicitud.contrasenaNueva().equals(solicitud.confirmarContrasena())) {
            throw new SolicitudInvalidaException("Las contrasenas no coinciden.");
        }

        TokenAcceso token = buscarTokenVigenteOLanzar(solicitud.token());
        Usuario usuario = token.getUsuario();
        if (!esElegibleParaRecuperacion(usuario)) {
            throw new TokenInvalidoException("El enlace de recuperacion ha expirado o no es valido.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.establecerContrasena(passwordEncoder.encode(solicitud.contrasenaNueva()), ahora);
        token.consumir(ahora);

        List<TokenAcceso> otrosVigentes = repositoryTokenAcceso
                .findByUsuarioIdAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
                        usuario.getId(), PropositoToken.RECUPERACION_CONTRASENA);
        otrosVigentes.stream()
                .filter(otro -> !otro.getId().equals(token.getId()))
                .forEach(otro -> otro.revocar(ahora, "Reemplazado por un restablecimiento exitoso."));

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(usuario, "CONTRASENA_RESTABLECIDA", "Usuario", "Contrasena restablecida via recuperacion.")
                .conEntidad(usuario.getId()));

        return new MensajeDto("Tu contrasena fue restablecida exitosamente.");
    }

    private boolean esElegibleParaRecuperacion(Usuario usuario) {
        return usuario.getEstadoAdministrativo() == EstadoAdministrativo.ACTIVO
                && usuario.getEstadoActivacion() == EstadoActivacion.COMPLETADA;
    }

    private void emitirTokenYNotificar(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();

        List<TokenAcceso> vigentesPrevios = repositoryTokenAcceso
                .findByUsuarioIdAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
                        usuario.getId(), PropositoToken.RECUPERACION_CONTRASENA);
        vigentesPrevios.forEach(previo -> previo.revocar(ahora, "Reemplazado por una nueva solicitud de recuperacion."));

        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracion = ahora.plusMinutes(duracionMinutos);
        TokenAcceso token = TokenAcceso.deRecuperacionContrasena(UUID.randomUUID(), usuario, hash, ahora, expiracion);
        repositoryTokenAcceso.save(token);

        String enlace = urlRecuperacion + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Recibimos una solicitud para restablecer tu contrasena en FirmaYA. "
                + "Si fuiste tu, usa el siguiente enlace (valido por " + duracionMinutos + " minutos): " + enlace
                + ". Si no solicitaste este cambio, podes ignorar este mensaje.";
        // El token viaja solo por este correo; nunca se persiste (ver ServiceEnvioNotificaciones).
        String resumenParaRegistro = "Se envio un enlace de recuperacion de contrasena, valido por "
                + duracionMinutos + " minutos.";
        serviceEnvioNotificaciones.enviarCorreoOperativo(usuario, "RECUPERACION_CONTRASENA",
                "Recuperacion de contrasena - FirmaYA", cuerpoCorreo, resumenParaRegistro);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(usuario, "RECUPERACION_SOLICITADA", "Usuario", "Solicitud de recuperacion de contrasena.")
                .conEntidad(usuario.getId()));
    }

    private TokenAcceso buscarTokenVigenteOLanzar(String tokenPlano) {
        byte[] hash = serviceHashCredencial.hashear(tokenPlano);
        TokenAcceso token = repositoryTokenAcceso.findByHashTokenAndProposito(hash, PropositoToken.RECUPERACION_CONTRASENA)
                .orElseThrow(() -> new TokenInvalidoException("El enlace de recuperacion no es valido."));
        if (!token.estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de recuperacion ha expirado o no es valido.");
        }
        return token;
    }
}
