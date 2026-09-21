package com.firmaya.api.participantes.dto;

import com.firmaya.api.participantes.EstadoInvitacion;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitacionDto(
        UUID id,
        UUID idParticipante,
        EstadoInvitacion estado,
        OffsetDateTime fechaExpiracionEnlace,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaUltimoEnvio
) {
}
