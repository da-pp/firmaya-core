package com.firmaya.api.firma.dto;

import com.firmaya.api.contratos.EstadoContrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResultadoFirmaDto(
        UUID idFirma,
        UUID idSolicitud,
        UUID idContrato,
        UUID idVersion,
        String hashVersion,
        OffsetDateTime fechaFirma,
        String estadoFirmante,
        EstadoContrato estadoContrato,
        ProgresoFirmaDto progreso
) {
}
