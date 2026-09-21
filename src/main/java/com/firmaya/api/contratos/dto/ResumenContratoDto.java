package com.firmaya.api.contratos.dto;

import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.TipoContrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumenContratoDto(
        UUID id,
        String nombre,
        TipoContrato tipo,
        EstadoContrato estado,
        int numeroVersionActual,
        OffsetDateTime fechaActualizacion,
        UUID idResponsable
) {
    public static ResumenContratoDto desde(Contrato contrato, int numeroVersionActual) {
        return new ResumenContratoDto(
                contrato.getId(), contrato.getNombre(), contrato.getTipoContrato(), contrato.getEstado(),
                numeroVersionActual, contrato.getFechaUltimaActividad(), contrato.getResponsable().getId());
    }
}
