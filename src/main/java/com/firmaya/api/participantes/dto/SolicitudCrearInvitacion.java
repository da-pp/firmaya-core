package com.firmaya.api.participantes.dto;

import com.firmaya.api.participantes.RolParticipacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCrearInvitacion(
        @NotBlank(message = "El nombre de la parte es obligatorio")
        @Size(max = 150, message = "El nombre de la parte no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "Ingrese un correo electronico valido (ej. nombre@dominio.com)")
        @Size(max = 254, message = "El correo electronico no puede superar 254 caracteres")
        String correoElectronico,

        @NotNull(message = "El rol es obligatorio")
        RolParticipacion rol,

        @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
        String mensaje
) {
}
