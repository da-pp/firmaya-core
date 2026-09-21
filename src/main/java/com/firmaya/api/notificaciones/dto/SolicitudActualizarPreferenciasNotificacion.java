package com.firmaya.api.notificaciones.dto;

import com.firmaya.api.notificaciones.CanalNotificacion;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record SolicitudActualizarPreferenciasNotificacion(
        Map<String, Boolean> eventosHabilitados,
        @NotEmpty(message = "Debe seleccionar al menos un canal de notificacion.")
        List<CanalNotificacion> canales
) {
}
