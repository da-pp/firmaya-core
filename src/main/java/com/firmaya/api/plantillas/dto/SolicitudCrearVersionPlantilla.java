package com.firmaya.api.plantillas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SolicitudCrearVersionPlantilla(
        @NotNull(message = "La version base es obligatoria")
        UUID idVersionBase,

        @NotBlank(message = "El contenido es obligatorio")
        String contenido,

        @Valid
        List<DefinicionCampoDinamicoDto> campos,

        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario,

        Boolean permitirSinCamposDinamicos
) {
}
