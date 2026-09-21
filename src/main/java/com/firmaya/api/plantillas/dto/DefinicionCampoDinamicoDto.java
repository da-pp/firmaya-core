package com.firmaya.api.plantillas.dto;

import com.firmaya.api.plantillas.TipoDatoCampo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DefinicionCampoDinamicoDto(
        @NotBlank(message = "El nombre del marcador es obligatorio")
        @Size(max = 100, message = "El nombre del marcador no puede superar los 100 caracteres")
        String nombreMarcador,

        @NotBlank(message = "La etiqueta es obligatoria")
        @Size(max = 200, message = "La etiqueta no puede superar los 200 caracteres")
        String etiqueta,

        @NotNull(message = "El tipo de dato es obligatorio")
        TipoDatoCampo tipo,

        boolean obligatorio,

        String valorPredeterminado
) {
}
