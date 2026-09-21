package com.firmaya.api.usuarios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReenvioActivacionDto(UUID idUsuario, OffsetDateTime fechaExpiracionActivacion, String estadoEntrega) {
}
