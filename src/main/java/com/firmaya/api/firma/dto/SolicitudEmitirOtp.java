package com.firmaya.api.firma.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudEmitirOtp(
        @NotNull UUID idSolicitud,
        @NotNull UUID idAceptacion
) {
}
