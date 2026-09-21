package com.firmaya.api.comentarios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComentarioDto(
        UUID id,
        UUID idVersion,
        String nombreAutor,
        String texto,
        String textoSeleccionado,
        OffsetDateTime fechaCreacion
) {
}
