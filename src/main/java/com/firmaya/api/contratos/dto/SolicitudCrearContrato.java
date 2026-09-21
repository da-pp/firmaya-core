package com.firmaya.api.contratos.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record SolicitudCrearContrato(
        @NotNull UUID idPlantilla,
        @NotNull UUID idVersionPlantilla,

        @NotBlank(message = "El nombre del contrato es obligatorio")
        @Size(max = 200, message = "El nombre del contrato no puede superar 200 caracteres")
        String nombre,

        @NotBlank(message = "Las partes involucradas son obligatorias")
        @Size(max = 1000, message = "Las partes involucradas no pueden superar 1000 caracteres")
        String partesInvolucradas,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @FutureOrPresent(message = "La fecha de inicio debe ser igual o posterior a la fecha actual")
        LocalDate fechaInicio,

        LocalDate fechaExpiracion,

        @Size(max = 2000, message = "La descripcion de la propiedad no puede superar 2000 caracteres")
        String descripcionPropiedad,

        Map<String, Object> valores
) {
}
