package com.firmaya.api.firma.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudReenviarOtp(
        @NotNull UUID idSolicitud,
        @NotNull UUID idDesafio
) {
}
