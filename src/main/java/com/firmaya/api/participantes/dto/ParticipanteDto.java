package com.firmaya.api.participantes.dto;

import com.firmaya.api.participantes.RolParticipacion;
import java.util.UUID;

public record ParticipanteDto(
        UUID id,
        String nombre,
        String correoElectronico,
        RolParticipacion rol,
        String estadoInvitacion,
        boolean haFirmado,
        boolean congeladoEnProcesoActual
) {
}
