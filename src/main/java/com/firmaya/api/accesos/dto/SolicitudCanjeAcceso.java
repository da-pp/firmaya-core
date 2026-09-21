package com.firmaya.api.accesos.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudCanjeAcceso(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}
