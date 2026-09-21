package com.firmaya.api.accesos;

import com.firmaya.api.seguridad.ServicioHashCredencial;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la solicitud a partir de la cookie de sesion externa (CU-04, EP-09..EP-12).
 * Solo actua si el filtro de sesion interna no autentico ya la solicitud (cookies con
 * nombres distintos; una peticion legitima trae como mucho una de las dos).
 */
public class SesionExternaAuthenticationFilter extends OncePerRequestFilter {

    public static final String NOMBRE_COOKIE = "sesion_firmaya_externa";

    private final RepositorioSesionExterna repositorioSesionExterna;
    private final ServicioHashCredencial servicioHashCredencial;

    public SesionExternaAuthenticationFilter(RepositorioSesionExterna repositorioSesionExterna,
                                              ServicioHashCredencial servicioHashCredencial) {
        this.repositorioSesionExterna = repositorioSesionExterna;
        this.servicioHashCredencial = servicioHashCredencial;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            obtenerCredencial(request).ifPresent(this::autenticar);
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> obtenerCredencial(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return List.of(cookies).stream()
                .filter(cookie -> NOMBRE_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void autenticar(String credencialEnClaro) {
        byte[] hash = servicioHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime ahora = OffsetDateTime.now();
        repositorioSesionExterna.buscarPorHashCredencial(hash)
                .filter(sesion -> sesion.estaVigente(ahora))
                .ifPresent(this::establecerContexto);
    }

    private void establecerContexto(SesionExterna sesion) {
        var participante = sesion.getParticipante();
        ContextoParticipanteAutenticado principal = new ContextoParticipanteAutenticado(
                sesion.getId(), participante.getContrato().getId(), participante.getId(),
                participante.getRolParticipacion(), sesion.getProposito(), sesion.getFechaExpiracion());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANTE_EXTERNO"));
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
