package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.procesofirma.EstadoProcesoFirma;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumenProcesoFirmaDto(
        UUID id,
        UUID idContrato,
        EstadoProcesoFirma estado,
        OffsetDateTime fechaCancelacion,
        EstadoContrato estadoContrato
) {
}
