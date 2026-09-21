package com.firmaya.api.participantes.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmisionEnlaceConsultaDto(
        UUID idParticipante,
        String proposito,
        OffsetDateTime fechaExpiracion,
        String estadoEntrega
) {
}
