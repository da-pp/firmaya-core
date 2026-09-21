package com.firmaya.api.plantillas.dto;

import com.firmaya.api.contratos.TipoContrato;
import java.util.List;
import java.util.UUID;

public record FormularioPlantillaDto(
        UUID idPlantilla,
        UUID idVersion,
        String nombre,
        TipoContrato tipo,
        List<CampoDinamicoDto> campos
) {
}
