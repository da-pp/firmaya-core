package com.firmaya.api.plantillas.dto;

import com.firmaya.api.contratos.TipoContrato;
import java.util.UUID;

public record CatalogoPlantillaDto(
        UUID idPlantilla,
        UUID idVersionPlantilla,
        String nombre,
        TipoContrato tipo,
        String descripcion
) {
}
