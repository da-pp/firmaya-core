package com.firmaya.api.contratos.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VerificacionIntegridadDto(
        UUID idVersion,
        int numeroVersion,
        String hashRecalculado,
        String hashAlmacenado,
        String hashProporcionado,
        boolean integridadAlmacenamiento,
        boolean coincideHashProporcionado,
        OffsetDateTime fechaVerificacion
) {
}
