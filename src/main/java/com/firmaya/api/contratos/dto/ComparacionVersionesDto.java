package com.firmaya.api.contratos.dto;

import java.util.List;

public record ComparacionVersionesDto(
        ResumenVersionContratoDto origen,
        ResumenVersionContratoDto destino,
        List<DiferenciaDto> cambios,
        int cantidadCambios
) {
}
