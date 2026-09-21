package com.firmaya.api.recuperacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudCompletarRecuperacion(
        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La contrasena nueva es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula, un numero y un caracter especial."
        )
        String contrasenaNueva,

        @NotBlank(message = "Debe confirmar la contrasena nueva")
        String confirmarContrasena
) {
}
