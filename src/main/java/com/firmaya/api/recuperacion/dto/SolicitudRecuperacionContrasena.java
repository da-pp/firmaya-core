package com.firmaya.api.recuperacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacionContrasena(
        @NotBlank(message = "El campo correo electronico es obligatorio")
        @Email(message = "Ingrese un correo electronico valido (ej. nombre@dominio.com)")
        @Size(max = 254, message = "El correo electronico no puede superar 254 caracteres")
        String correoElectronico
) {
}
