package com.firmaya.api.plantillas.dto;

import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.EstadoPlantilla;
import java.util.UUID;

public record PlantillaAdministracionDto(
        UUID id,
        String nombre,
        TipoContrato tipo,
        String descripcion,
        EstadoPlantilla estado,
        UUID idVersionActual,
        int numeroVersion
) {
}
