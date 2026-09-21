package com.firmaya.api.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudInicioSesion(
        @NotBlank(message = "El campo correo electronico es obligatorio")
        @Email(message = "Ingrese un correo electronico valido")
        @Size(max = 254, message = "El correo electronico no puede superar 254 caracteres")
        String correoElectronico,

        @NotBlank(message = "El campo contrasena es obligatorio")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String contrasena
) {
}
