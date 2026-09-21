package com.firmaya.api.usuarios.dto;

import com.firmaya.api.usuarios.EstadoAdministrativo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCambiarEstadoUsuario(
        @NotNull(message = "El estado administrativo es obligatorio")
        EstadoAdministrativo estadoAdministrativo,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
