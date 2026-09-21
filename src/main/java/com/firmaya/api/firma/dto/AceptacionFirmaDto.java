package com.firmaya.api.firma.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AceptacionFirmaDto(
        UUID idAceptacion,
        UUID idSolicitud,
        UUID idVersion,
        OffsetDateTime fechaAceptacion,
        String estado
) {
}
