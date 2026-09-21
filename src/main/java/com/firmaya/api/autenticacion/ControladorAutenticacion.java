package com.firmaya.api.autenticacion;

import com.firmaya.api.autenticacion.dto.RespuestaInicioSesion;
import com.firmaya.api.autenticacion.dto.SolicitudInicioSesion;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import com.firmaya.api.seguridad.SesionInternaAuthenticationFilter;
import com.firmaya.api.usuarios.dto.ResumenUsuarioDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-19, EP-01..EP-03. */
@RestController
@RequestMapping("/api/v1/autenticacion")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;

    public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud,
                                                                 HttpServletRequest request) {
        ResultadoInicioSesion resultado = servicioAutenticacion.iniciarSesion(
                solicitud.correoElectronico(), solicitud.contrasena(),
                request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));

        ResponseCookie cookie = construirCookieSesion(resultado.credencialSesionEnClaro(), resultado.fechaExpiracion());
        var cuerpo = new RespuestaInicioSesion(
                ResumenUsuarioDto.desde(resultado.usuario()), resultado.fechaExpiracion());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(cuerpo);
    }

    @GetMapping("/mi")
    public ResumenUsuarioDto obtenerUsuarioActual(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioAutenticacion.obtenerUsuarioActual(usuario.idUsuario());
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        obtenerCredencial(request).ifPresent(servicioAutenticacion::cerrarSesionActual);
        ResponseCookie cookieVacia = ResponseCookie.from(SesionInternaAuthenticationFilter.NOMBRE_COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(Duration.ZERO).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookieVacia.toString()).build();
    }

    private ResponseCookie construirCookieSesion(String credencial, OffsetDateTime expiracion) {
        Duration duracion = Duration.between(OffsetDateTime.now(), expiracion);
        return ResponseCookie.from(SesionInternaAuthenticationFilter.NOMBRE_COOKIE, credencial)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duracion.isNegative() ? Duration.ZERO : duracion)
                .build();
    }

    private Optional<String> obtenerCredencial(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (SesionInternaAuthenticationFilter.NOMBRE_COOKIE.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
