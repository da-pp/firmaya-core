package com.firmaya.api.contratos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SolicitudRestaurarVersionContrato(
        @NotNull(message = "La version base es obligatoria")
        UUID idVersionBase,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
