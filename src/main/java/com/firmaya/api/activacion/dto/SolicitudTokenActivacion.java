package com.firmaya.api.activacion.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudTokenActivacion(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}
