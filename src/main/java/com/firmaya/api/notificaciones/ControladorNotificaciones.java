package com.firmaya.api.notificaciones;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.notificaciones.dto.NotificacionPlataformaDto;
import com.firmaya.api.notificaciones.dto.PreferenciasNotificacionDto;
import com.firmaya.api.notificaciones.dto.SolicitudActualizarPreferenciasNotificacion;
import com.firmaya.api.notificaciones.dto.TipoEventoNotificacionDto;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU-20, EP-59..EP-63. Todas las operaciones actuan sobre el usuario autenticado. */
@RestController
@RequestMapping("/api/v1/mi")
public class ControladorNotificaciones {

    private final ServicioPreferenciasNotificacion servicioPreferenciasNotificacion;
    private final ServicioBandejaNotificaciones servicioBandejaNotificaciones;

    public ControladorNotificaciones(ServicioPreferenciasNotificacion servicioPreferenciasNotificacion,
                                      ServicioBandejaNotificaciones servicioBandejaNotificaciones) {
        this.servicioPreferenciasNotificacion = servicioPreferenciasNotificacion;
        this.servicioBandejaNotificaciones = servicioBandejaNotificaciones;
    }

    @GetMapping("/tipos-evento-notificacion")
    public List<TipoEventoNotificacionDto> obtenerTiposEvento() {
        return servicioPreferenciasNotificacion.obtenerTiposEvento();
    }

    @GetMapping("/preferencias-notificacion")
    public PreferenciasNotificacionDto obtenerMisPreferencias(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioPreferenciasNotificacion.obtenerMisPreferencias(usuario.idUsuario());
    }

    @PutMapping("/preferencias-notificacion")
    public PreferenciasNotificacionDto reemplazarMisPreferencias(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                                   @Valid @RequestBody SolicitudActualizarPreferenciasNotificacion solicitud) {
        return servicioPreferenciasNotificacion.reemplazarMisPreferencias(usuario.idUsuario(), solicitud);
    }

    @GetMapping("/notificaciones")
    public PaginaDto<NotificacionPlataformaDto> listarMisNotificaciones(
            @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Boolean soloNoLeidas) {
        return servicioBandejaNotificaciones.listarMias(usuario.idUsuario(), pagina, soloNoLeidas);
    }

    @PatchMapping("/notificaciones/{idNotificacion}/leida")
    public NotificacionPlataformaDto marcarLeida(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                  @PathVariable UUID idNotificacion) {
        return servicioBandejaNotificaciones.marcarLeida(usuario.idUsuario(), idNotificacion);
    }
}
