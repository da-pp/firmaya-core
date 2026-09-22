package com.firmaya.api.accesos;

import com.firmaya.api.accesos.dto.AccesoExternoDto;
import com.firmaya.api.accesos.dto.SesionExternaDto;
import com.firmaya.api.accesos.dto.SolicitudCanjeAcceso;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-04, EP-09..EP-12. */
@RestController
@RequestMapping("/api/v1/accesos")
public class ControllerAccesoExterno {

    private final ServiceAccesoExterno serviceAccesoExterno;

    public ControllerAccesoExterno(ServiceAccesoExterno serviceAccesoExterno) {
        this.serviceAccesoExterno = serviceAccesoExterno;
    }

    @PostMapping("/canjear")
    public ResponseEntity<SesionExternaDto> canjear(@Valid @RequestBody SolicitudCanjeAcceso solicitud,
                                                      HttpServletRequest request) {
        var resultado = serviceAccesoExterno.canjear(solicitud.token(), request.getRemoteAddr());
        ResponseCookie cookie = construirCookie(resultado.credencialEnClaro(), resultado.sesion().fechaExpiracion());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(resultado.sesion());
    }

    @GetMapping("/mi")
    public AccesoExternoDto obtenerContexto(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto) {
        return serviceAccesoExterno.obtenerContexto(contexto);
    }

    /**
     * Extiende la vigencia de la sesion en el servidor; la credencial (cookie) no cambia, asi
     * que no se reemite Set-Cookie aqui. La sincronizacion exacta del maxAge del lado del
     * navegador frente a la extension del lado del servidor sigue [PENDIENTE] (PD-02).
     */
    @PostMapping("/sesion/extender")
    public SesionExternaDto extenderSesion(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto) {
        return serviceAccesoExterno.extenderSesion(contexto);
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto) {
        serviceAccesoExterno.cerrarSesion(contexto);
        ResponseCookie cookieVacia = ResponseCookie.from(SesionExternaAuthenticationFilter.NOMBRE_COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(Duration.ZERO).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookieVacia.toString()).build();
    }

    private ResponseCookie construirCookie(String credencial, OffsetDateTime expiracion) {
        Duration duracion = Duration.between(OffsetDateTime.now(), expiracion);
        return ResponseCookie.from(SesionExternaAuthenticationFilter.NOMBRE_COOKIE, credencial)
                .httpOnly(true).secure(true).sameSite("Strict").path("/")
                .maxAge(duracion.isNegative() ? Duration.ZERO : duracion)
                .build();
    }
}
