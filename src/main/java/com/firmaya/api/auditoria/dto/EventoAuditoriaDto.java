package com.firmaya.api.auditoria.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoAuditoriaDto(
        UUID id,
        OffsetDateTime fechaEvento,
        String actor,
        String tipoAccion,
        String tipoEntidad,
        UUID idEntidad,
        String descripcion,
        String ipEnmascarada
) {
}
