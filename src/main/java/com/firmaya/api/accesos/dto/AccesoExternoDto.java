package com.firmaya.api.accesos.dto;

import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.participantes.RolParticipacion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AccesoExternoDto(
        UUID idContrato,
        UUID idParticipante,
        RolParticipacion rolParticipante,
        PropositoAcceso proposito,
        List<String> accionesPermitidas,
        OffsetDateTime fechaExpiracionSesion
) {
}
