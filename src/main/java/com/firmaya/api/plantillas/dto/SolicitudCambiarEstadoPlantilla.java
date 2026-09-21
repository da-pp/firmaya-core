package com.firmaya.api.plantillas.dto;

import com.firmaya.api.plantillas.EstadoPlantilla;
import jakarta.validation.constraints.NotNull;

public record SolicitudCambiarEstadoPlantilla(
        @NotNull(message = "El estado es obligatorio")
        EstadoPlantilla estado
) {
}
