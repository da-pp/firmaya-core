package com.firmaya.api.plantillas.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumenVersionPlantillaDto(UUID id, int numeroVersion, OffsetDateTime fechaCreacion, UUID idAutor) {
}
