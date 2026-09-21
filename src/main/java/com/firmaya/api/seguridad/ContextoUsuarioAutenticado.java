package com.firmaya.api.seguridad;

import com.firmaya.api.usuarios.RolGlobal;
import java.util.UUID;

/** Principal ligero puesto en el SecurityContext por {@link SesionInternaAuthenticationFilter}. */
public record ContextoUsuarioAutenticado(UUID idUsuario, String correoElectronico, RolGlobal rolGlobal) {
}
