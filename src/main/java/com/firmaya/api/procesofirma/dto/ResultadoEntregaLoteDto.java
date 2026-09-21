package com.firmaya.api.procesofirma.dto;

import java.util.List;

public record ResultadoEntregaLoteDto(
        int reintentados,
        int entregados,
        int fallidos,
        List<ElementoEntregaDto> detalles
) {
}
