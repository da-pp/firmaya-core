package com.firmaya.api.usuarios.dto;

import com.firmaya.api.usuarios.EstadoActivacion;
import com.firmaya.api.usuarios.EstadoAdministrativo;
import com.firmaya.api.usuarios.RolGlobal;
import com.firmaya.api.usuarios.Usuario;
import java.util.UUID;

public record ResumenUsuarioDto(
        UUID id,
        String nombre,
        String apellido,
        String correoElectronico,
        RolGlobal rolGlobal,
        EstadoAdministrativo estadoAdministrativo,
        EstadoActivacion estadoActivacion
) {
    public static ResumenUsuarioDto desde(Usuario usuario) {
        return new ResumenUsuarioDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreoElectronico(),
                usuario.getRolGlobal(),
                usuario.getEstadoAdministrativo(),
                usuario.getEstadoActivacion()
        );
    }
}
