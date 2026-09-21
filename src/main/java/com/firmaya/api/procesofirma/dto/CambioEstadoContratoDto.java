package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.contratos.EstadoContrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CambioEstadoContratoDto(
        UUID idContrato,
        EstadoContrato estadoAnterior,
        EstadoContrato estadoNuevo,
        OffsetDateTime fechaCambio,
        UUID idVersionActual,
        UUID idProcesoFirma
) {
}
