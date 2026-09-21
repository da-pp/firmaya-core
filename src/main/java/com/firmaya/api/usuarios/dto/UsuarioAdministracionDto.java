package com.firmaya.api.usuarios.dto;

import com.firmaya.api.usuarios.EstadoActivacion;
import com.firmaya.api.usuarios.EstadoAdministrativo;
import com.firmaya.api.usuarios.RolGlobal;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioAdministracionDto(
        UUID id,
        String nombre,
        String apellido,
        String correoElectronico,
        RolGlobal rolGlobal,
        EstadoAdministrativo estadoAdministrativo,
        EstadoActivacion estadoActivacion,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActivacion
) {
    public static UsuarioAdministracionDto desde(Usuario usuario) {
        return new UsuarioAdministracionDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreoElectronico(),
                usuario.getRolGlobal(),
                usuario.getEstadoAdministrativo(),
                usuario.getEstadoActivacion(),
                usuario.getFechaCreacion(),
                usuario.getFechaActivacion());
    }
}
