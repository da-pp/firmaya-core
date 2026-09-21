package com.firmaya.api.accesos.dto;

import com.firmaya.api.accesos.PropositoAcceso;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SesionExternaDto(
        UUID idContrato,
        UUID idParticipante,
        PropositoAcceso proposito,
        OffsetDateTime fechaExpiracion,
        List<String> accionesPermitidas
) {
}
