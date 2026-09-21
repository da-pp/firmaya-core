package com.firmaya.api.panelactividad.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FilaContratoPanelDto(
        UUID idContrato,
        String nombre,
        String estado,
        String nombreResponsable,
        OffsetDateTime fechaActualizacion
) {
}
