package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.procesofirma.EstadoProcesoFirma;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DetalleProcesoFirmaDto(
        UUID id,
        UUID idContrato,
        EstadoProcesoFirma estado,
        UUID idVersionObjetivo,
        String hashObjetivo,
        List<FirmanteCongeladoDto> firmantesCongelados,
        int cantidadCompletadas,
        int cantidadTotal,
        LocalDate fechaLimite
) {
}
