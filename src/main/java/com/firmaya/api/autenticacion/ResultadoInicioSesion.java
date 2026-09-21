package com.firmaya.api.autenticacion;

import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;

public record ResultadoInicioSesion(Usuario usuario, String credencialSesionEnClaro, OffsetDateTime fechaExpiracion) {
}
