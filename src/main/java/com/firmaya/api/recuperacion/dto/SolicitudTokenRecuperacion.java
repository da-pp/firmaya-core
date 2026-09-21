package com.firmaya.api.recuperacion.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudTokenRecuperacion(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}
