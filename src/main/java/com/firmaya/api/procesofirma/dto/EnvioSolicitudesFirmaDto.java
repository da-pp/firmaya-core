package com.firmaya.api.procesofirma.dto;

import java.util.List;
import java.util.UUID;

public record EnvioSolicitudesFirmaDto(
        UUID idProceso,
        List<ElementoEnvioSolicitudDto> solicitudes,
        int cantidadCompletadas,
        int cantidadTotal
) {
}
