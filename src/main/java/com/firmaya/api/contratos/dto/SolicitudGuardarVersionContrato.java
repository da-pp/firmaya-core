package com.firmaya.api.contratos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SolicitudGuardarVersionContrato(
        @NotNull(message = "El identificador de version base es obligatorio")
        UUID idVersionBase,

        @NotBlank(message = "El contenido del contrato es obligatorio")
        @Size(min = 100, message = "El contenido del contrato debe tener al menos 100 caracteres")
        String contenido,

        @Size(max = 500, message = "El comentario de version no puede superar 500 caracteres")
        String comentarioCambio
) {
}
