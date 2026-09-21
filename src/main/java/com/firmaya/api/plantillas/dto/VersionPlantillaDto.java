package com.firmaya.api.plantillas.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VersionPlantillaDto(
        UUID id,
        UUID idPlantilla,
        int numeroVersion,
        String contenido,
        List<CampoDinamicoDto> campos,
        OffsetDateTime fechaCreacion,
        UUID idAutor
) {
}
