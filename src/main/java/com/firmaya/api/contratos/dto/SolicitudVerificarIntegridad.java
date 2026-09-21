package com.firmaya.api.contratos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record SolicitudVerificarIntegridad(
        UUID idVersion,

        @NotBlank(message = "El hash a verificar es obligatorio")
        @Pattern(regexp = "^[0-9a-fA-F]{64}$",
                message = "El hash debe tener exactamente 64 caracteres hexadecimales (0-9, a-f).")
        String hashProporcionado
) {
}
