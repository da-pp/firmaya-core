package com.firmaya.api.notificaciones.dto;

import com.firmaya.api.notificaciones.CanalNotificacion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record PreferenciasNotificacionDto(
        Map<String, Boolean> eventosHabilitados,
        List<CanalNotificacion> canales,
        OffsetDateTime fechaActualizacion
) {
}
