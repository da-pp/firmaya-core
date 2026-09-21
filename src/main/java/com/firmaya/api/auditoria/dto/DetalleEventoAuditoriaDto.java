package com.firmaya.api.auditoria.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DetalleEventoAuditoriaDto(
        UUID id,
        OffsetDateTime fechaEvento,
        String actor,
        String tipoAccion,
        String tipoEntidad,
        UUID idEntidad,
        String descripcion,
        Object antes,
        Object despues,
        UUID idVersionContrato,
        String hashSha256,
        String ip
) {
}
