package com.firmaya.api.activacion.dto;

import java.time.OffsetDateTime;

public record ValidacionActivacionDto(boolean valido, OffsetDateTime fechaExpiracion) {
}
