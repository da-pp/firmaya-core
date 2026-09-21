package com.firmaya.api.accesos;

import com.firmaya.api.participantes.RolParticipacion;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Principal ligero puesto en el SecurityContext por {@link SesionExternaAuthenticationFilter}. */
public record ContextoParticipanteAutenticado(
        UUID idSesionExterna,
        UUID idContrato,
        UUID idParticipante,
        RolParticipacion rolParticipacion,
        PropositoAcceso proposito,
        OffsetDateTime fechaExpiracionSesion
) {
}
