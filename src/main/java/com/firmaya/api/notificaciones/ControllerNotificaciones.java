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
public class ControllerNotificaciones {

    private final ServicePreferenciasNotificacion servicePreferenciasNotificacion;
    private final ServiceBandejaNotificaciones serviceBandejaNotificaciones;

    public ControllerNotificaciones(ServicePreferenciasNotificacion servicePreferenciasNotificacion,
                                      ServiceBandejaNotificaciones serviceBandejaNotificaciones) {
        this.servicePreferenciasNotificacion = servicePreferenciasNotificacion;
        this.serviceBandejaNotificaciones = serviceBandejaNotificaciones;
    }

    @GetMapping("/tipos-evento-notificacion")
    public List<TipoEventoNotificacionDto> obtenerTiposEvento() {
        return servicePreferenciasNotificacion.obtenerTiposEvento();
    }

    @GetMapping("/preferencias-notificacion")
    public PreferenciasNotificacionDto obtenerMisPreferencias(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicePreferenciasNotificacion.obtenerMisPreferencias(usuario.idUsuario());
    }

    @PutMapping("/preferencias-notificacion")
    public PreferenciasNotificacionDto reemplazarMisPreferencias(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                                   @Valid @RequestBody SolicitudActualizarPreferenciasNotificacion solicitud) {
        return servicePreferenciasNotificacion.reemplazarMisPreferencias(usuario.idUsuario(), solicitud);
    }

    @GetMapping("/notificaciones")
    public PaginaDto<NotificacionPlataformaDto> listarMisNotificaciones(
            @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Boolean soloNoLeidas) {
        return serviceBandejaNotificaciones.listarMias(usuario.idUsuario(), pagina, soloNoLeidas);
    }

    @PatchMapping("/notificaciones/{idNotificacion}/leida")
    public NotificacionPlataformaDto marcarLeida(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                  @PathVariable UUID idNotificacion) {
        return serviceBandejaNotificaciones.marcarLeida(usuario.idUsuario(), idNotificacion);
    }
}
