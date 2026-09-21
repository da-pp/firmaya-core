package com.firmaya.api.plantillas.dto;

import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.EstadoPlantilla;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SolicitudCrearPlantilla(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
        String nombre,

        @NotNull(message = "El tipo de contrato es obligatorio")
        TipoContrato tipo,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String descripcion,

        @NotBlank(message = "El contenido es obligatorio")
        String contenido,

        @NotNull(message = "El estado es obligatorio")
        EstadoPlantilla estado,

        @Valid
        List<DefinicionCampoDinamicoDto> campos,

        Boolean permitirSinCamposDinamicos
) {
}
