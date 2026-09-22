package com.firmaya.api.autenticacion;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.CredencialesInvalidasException;
import com.firmaya.api.comun.excepciones.CuentaBloqueadaException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.seguridad.RepositorioSesionInterna;
import com.firmaya.api.seguridad.ServicioHashCredencial;
import com.firmaya.api.seguridad.SesionInterna;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import com.firmaya.api.usuarios.dto.ResumenUsuarioDto;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-19: iniciar sesion, consultar identidad y cerrar sesion (EP-01..EP-03). */
@Service
@Transactional
public class ServicioAutenticacion {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioSesionInterna repositorioSesionInterna;
    private final ServicioHashCredencial servicioHashCredencial;
    private final PasswordEncoder passwordEncoder;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final int maximoIntentos;
    private final int minutosBloqueo;
    private final int duracionSesionHoras;

    public ServicioAutenticacion(RepositorioUsuario repositorioUsuario,
                                  RepositorioSesionInterna repositorioSesionInterna,
                                  ServicioHashCredencial servicioHashCredencial,
                                  PasswordEncoder passwordEncoder,
                                  ServicioRegistroAuditoria servicioRegistroAuditoria,
                                  @Value("${firmaya.login.intentos-maximos}") int maximoIntentos,
                                  @Value("${firmaya.login.bloqueo-minutos}") int minutosBloqueo,
                                  @Value("${firmaya.sesion.duracion-horas}") int duracionSesionHoras) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioSesionInterna = repositorioSesionInterna;
        this.servicioHashCredencial = servicioHashCredencial;
        this.passwordEncoder = passwordEncoder;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.maximoIntentos = maximoIntentos;
        this.minutosBloqueo = minutosBloqueo;
        this.duracionSesionHoras = duracionSesionHoras;
    }

    // El metodo lanza CredencialesInvalidasException/CuentaBloqueadaException en varios
    // caminos de fallo, pero el incremento de intentos_inicio_fallidos (y el bloqueo
    // resultante a los 5 intentos, CU-19) debe persistir igual: sin noRollbackFor, la
    // regla por defecto de @Transactional revierte esa mutacion junto con la excepcion,
    // dejando el contador siempre en 0 y el bloqueo por intentos fallidos inalcanzable.
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public ResultadoInicioSesion iniciarSesion(String correoElectronico, String contrasena, String direccionIp,
                                                String agenteUsuario) {
        String correoNormalizado = correoElectronico.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = repositorioUsuario.findByCorreoNormalizado(correoNormalizado).orElse(null);

        // Cuenta inexistente: mensaje generico, sin tocar ningun contador (no hay cuenta que bloquear).
        if (usuario == null) {
            servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deSistema("INICIO_SESION_FALLIDO", "Usuario", "Intento de inicio de sesion con correo no registrado.")
                    .conIp(direccionIp));
            throw new CredencialesInvalidasException("El correo electronico o la contrasena son incorrectos.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        if (usuario.estaBloqueada(ahora)) {
            throw new CuentaBloqueadaException(
                    "Tu cuenta ha sido bloqueada temporalmente. Puedes intentarlo de nuevo en "
                            + minutosBloqueo + " minutos o recuperar tu contrasena.",
                    usuario.getBloqueoHasta());
        }

        // Cuenta deshabilitada o sin activacion completa: mismo mensaje generico (no revela estado).
        if (!usuario.habilitadaParaIniciarSesion() || !passwordEncoder.matches(contrasena, usuario.getHashContrasena())) {
            if (usuario.habilitadaParaIniciarSesion()) {
                usuario.registrarIntentoFallido(maximoIntentos, minutosBloqueo, ahora);
            }
            servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deUsuario(usuario, "INICIO_SESION_FALLIDO", "Usuario", "Intento de inicio de sesion fallido.")
                    .conEntidad(usuario.getId())
                    .conIp(direccionIp));
            throw new CredencialesInvalidasException("El correo electronico o la contrasena son incorrectos.");
        }

        usuario.registrarIniciosSesionExitoso(ahora);

        String credencialEnClaro = servicioHashCredencial.generarCredencialAleatoria();
        byte[] hash = servicioHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracion = ahora.plusHours(duracionSesionHoras);
        SesionInterna sesion = new SesionInterna(UUID.randomUUID(), usuario, hash, ahora, expiracion,
                direccionIp, agenteUsuario);
        repositorioSesionInterna.save(sesion);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(usuario, "INICIO_SESION_EXITOSO", "Usuario", "Inicio de sesion exitoso.")
                .conEntidad(usuario.getId())
                .conIp(direccionIp));

        return new ResultadoInicioSesion(usuario, credencialEnClaro, expiracion);
    }

    @Transactional(readOnly = true)
    public ResumenUsuarioDto obtenerUsuarioActual(UUID idUsuario) {
        Usuario usuario = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        return ResumenUsuarioDto.desde(usuario);
    }

    public void cerrarSesionActual(String credencialEnClaro) {
        if (credencialEnClaro == null) {
            return;
        }
        byte[] hash = servicioHashCredencial.hashear(credencialEnClaro);
        repositorioSesionInterna.findByHashCredencial(hash).ifPresent(sesion -> {
            sesion.revocar(OffsetDateTime.now());
            servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deUsuario(sesion.getUsuario(), "CIERRE_SESION", "SesionInterna", "Cierre de sesion.")
                    .conEntidad(sesion.getId()));
        });
    }
}
