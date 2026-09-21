package com.firmaya.api.firma.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DesafioOtpDto(
        UUID idDesafio,
        String correoEnmascarado,
        OffsetDateTime fechaExpiracion,
        int intentosRestantes
) {
}
