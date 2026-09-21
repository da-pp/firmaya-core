package com.firmaya.api.activacion.dto;

import java.util.UUID;

public record ResultadoActivacionDto(boolean activado, UUID idUsuario) {
}
