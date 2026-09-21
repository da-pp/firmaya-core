package com.firmaya.api.notificaciones.dto;

import com.firmaya.api.notificaciones.CanalNotificacion;
import java.util.List;

public record TipoEventoNotificacionDto(
        String codigo,
        String etiqueta,
        boolean configurable,
        List<CanalNotificacion> canalesDisponibles
) {
}
