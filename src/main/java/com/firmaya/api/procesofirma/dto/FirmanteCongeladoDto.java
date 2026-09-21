package com.firmaya.api.procesofirma.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FirmanteCongeladoDto(
        UUID idParticipante,
        String nombre,
        String estadoFirma,
        String estadoSolicitud,
        OffsetDateTime fechaEvento
) {
}
