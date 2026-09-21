package com.firmaya.api.procesofirma.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SolicitudFirmaDto(
        UUID id,
        UUID idParticipante,
        String estado,
        OffsetDateTime fechaUltimoEnvio,
        LocalDate fechaLimite,
        OffsetDateTime fechaExpiracion,
        String estadoEntrega
) {
}
