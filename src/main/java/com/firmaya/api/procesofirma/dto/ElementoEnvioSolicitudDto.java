package com.firmaya.api.procesofirma.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ElementoEnvioSolicitudDto(
        UUID idSolicitud,
        UUID idParticipante,
        String estado,
        OffsetDateTime fechaExpiracion
) {
}
