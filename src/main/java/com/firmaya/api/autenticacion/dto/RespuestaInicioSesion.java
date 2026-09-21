package com.firmaya.api.autenticacion.dto;

import com.firmaya.api.usuarios.dto.ResumenUsuarioDto;
import java.time.OffsetDateTime;

public record RespuestaInicioSesion(ResumenUsuarioDto usuario, OffsetDateTime fechaExpiracionSesion) {
}
