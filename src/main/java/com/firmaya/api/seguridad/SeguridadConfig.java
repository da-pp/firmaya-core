package com.firmaya.api.seguridad;

import com.firmaya.api.accesos.RepositorioSesionExterna;
import com.firmaya.api.accesos.SesionExternaAuthenticationFilter;
import com.firmaya.api.comun.ErrorApiDto;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de FirmaYA: sin HttpSession de servlet (STATELESS) y autenticacion propia por
 * cookie de sesion interna (ver {@link SesionInternaAuthenticationFilter}), acorde al
 * "mecanismo de sesion configurado [DISEÑO]" descripto en EP-01.
 *
 * CSRF: se apoya en la cookie de sesion con atributo SameSite=Strict (ver
 * ServicioAutenticacion), que mitiga el escenario clasico de CSRF entre sitios en
 * navegadores modernos; la politica CSRF definitiva sigue marcada [DISEÑO] en la
 * especificacion (EP-03) y puede reforzarse con un token de cabecera cuando exista un
 * frontend concreto que lo consuma.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final ObjectMapper objectMapper;

    // Origen del frontend Next.js en desarrollo; sin despliegue definido aun, se usa este
    // valor de referencia (analogo a las URLs [PENDIENTE] de correo) documentado para revision.
    @Value("${firmaya.frontend.origen-cors:http://localhost:3000}")
    private String origenCorsFrontend;

    public SeguridadConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(origenCorsFrontend));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Content-Type"));
        // Necesario para que el navegador envie/reciba la cookie de sesion (httpOnly) entre
        // el frontend (puerto 3000) y esta API (puerto 8080): mismo "site" (localhost), origen distinto.
        configuracion.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/v1/**", configuracion);
        return fuente;
    }

    @Bean
    public SesionInternaAuthenticationFilter sesionInternaAuthenticationFilter(
            RepositorioSesionInterna repositorioSesionInterna, ServicioHashCredencial servicioHashCredencial) {
        return new SesionInternaAuthenticationFilter(repositorioSesionInterna, servicioHashCredencial);
    }

    @Bean
    public SesionExternaAuthenticationFilter sesionExternaAuthenticationFilter(
            RepositorioSesionExterna repositorioSesionExterna, ServicioHashCredencial servicioHashCredencial) {
        return new SesionExternaAuthenticationFilter(repositorioSesionExterna, servicioHashCredencial);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     SesionInternaAuthenticationFilter sesionInternaAuthenticationFilter,
                                                     SesionExternaAuthenticationFilter sesionExternaAuthenticationFilter)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/iniciar-sesion")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/recuperacion-contrasena")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/recuperacion-contrasena/validar")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/recuperacion-contrasena/completar")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/activaciones/validar")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/activaciones/completar")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accesos/canjear")
                        .permitAll()
                        .requestMatchers("/api/v1/administracion/**")
                        .hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(sesionInternaAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sesionExternaAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(this::responderNoAutenticado)
                        .accessDeniedHandler((request, response, ex) -> responderAccesoDenegado(response)));
        return http.build();
    }

    private void responderNoAutenticado(jakarta.servlet.http.HttpServletRequest request,
                                         HttpServletResponse response,
                                         org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                ErrorApiDto.de("NO_AUTENTICADO", "Debe iniciar sesion para acceder a este recurso."));
    }

    private void responderAccesoDenegado(HttpServletResponse response) throws java.io.IOException {
        escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                ErrorApiDto.de("ACCESO_DENEGADO", "No tiene permisos para esta operacion."));
    }

    private void escribirError(HttpServletResponse response, int estadoHttp, ErrorApiDto error) throws java.io.IOException {
        response.setStatus(estadoHttp);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
