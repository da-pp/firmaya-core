package com.firmaya.api.seguridad;

import com.firmaya.api.usuarios.EstadoAdministrativo;
import com.firmaya.api.usuarios.Usuario;
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
 * Autentica la solicitud a partir de la cookie de sesion interna (EP-01..EP-03, CU-19).
 * No usa HttpSession de servlet: la credencial vive en firmaya.SesionInterna, es opaca y
 * revocable, y su hash (nunca el valor en claro) es lo que se busca en base de datos.
 */
public class SesionInternaAuthenticationFilter extends OncePerRequestFilter {

    public static final String NOMBRE_COOKIE = "sesion_firmaya";

    private final RepositorioSesionInterna repositorioSesionInterna;
    private final ServicioHashCredencial servicioHashCredencial;

    public SesionInternaAuthenticationFilter(RepositorioSesionInterna repositorioSesionInterna,
                                              ServicioHashCredencial servicioHashCredencial) {
        this.repositorioSesionInterna = repositorioSesionInterna;
        this.servicioHashCredencial = servicioHashCredencial;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        obtenerCredencial(request).ifPresent(credencial -> autenticar(credencial));
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
        repositorioSesionInterna.findByHashCredencial(hash)
                .filter(sesion -> sesion.estaVigente(ahora))
                .map(SesionInterna::getUsuario)
                .filter(usuario -> usuario.getEstadoAdministrativo() == EstadoAdministrativo.ACTIVO)
                .ifPresent(this::establecerContexto);
    }

    private void establecerContexto(Usuario usuario) {
        ContextoUsuarioAutenticado principal = new ContextoUsuarioAutenticado(
                usuario.getId(), usuario.getCorreoElectronico(), usuario.getRolGlobal());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRolGlobal().name()));
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
