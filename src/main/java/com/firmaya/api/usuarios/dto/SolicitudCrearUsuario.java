package com.firmaya.api.usuarios.dto;

import com.firmaya.api.usuarios.EstadoAdministrativo;
import com.firmaya.api.usuarios.RolGlobal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCrearUsuario(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        @Size(max = 254, message = "El correo electronico no puede superar los 254 caracteres")
        String correoElectronico,

        @NotNull(message = "El rol es obligatorio")
        RolGlobal rolGlobal,

        @NotNull(message = "El estado administrativo es obligatorio")
        EstadoAdministrativo estadoAdministrativo
) {
}
