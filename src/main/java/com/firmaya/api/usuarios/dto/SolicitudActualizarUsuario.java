package com.firmaya.api.usuarios.dto;

import com.firmaya.api.usuarios.RolGlobal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** CU-15, EP-16: todos los campos son opcionales; solo se actualizan los presentes. */
public record SolicitudActualizarUsuario(
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @Email(message = "El correo electronico no tiene un formato valido")
        @Size(max = 254, message = "El correo electronico no puede superar los 254 caracteres")
        String correoElectronico,

        RolGlobal rolGlobal
) {
}
