package com.firmaya.api.recuperacion.dto;

import java.time.OffsetDateTime;

public record ValidacionRecuperacionDto(boolean valido, OffsetDateTime fechaExpiracion) {
}
