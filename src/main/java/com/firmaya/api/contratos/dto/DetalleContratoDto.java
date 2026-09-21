package com.firmaya.api.contratos.dto;

import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.TipoContrato;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DetalleContratoDto(
        UUID id,
        String nombre,
        TipoContrato tipo,
        String partesInvolucradas,
        EstadoContrato estado,
        UUID idResponsable,
        UUID idVersionPlantilla,
        VersionContratoDto versionActual,
        LocalDate fechaInicio,
        LocalDate fechaExpiracion,
        String descripcionPropiedad,
        OffsetDateTime fechaCreacion,
        List<String> permisos
) {
}
