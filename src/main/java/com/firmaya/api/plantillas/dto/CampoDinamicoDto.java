package com.firmaya.api.plantillas.dto;

import com.firmaya.api.plantillas.TipoDatoCampo;

public record CampoDinamicoDto(
        String nombreMarcador,
        String etiqueta,
        TipoDatoCampo tipo,
        boolean obligatorio,
        String valorPredeterminado
) {
}
