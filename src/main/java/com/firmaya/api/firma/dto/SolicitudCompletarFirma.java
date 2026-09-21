package com.firmaya.api.firma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record SolicitudCompletarFirma(
        @NotNull UUID idSolicitud,
        @NotNull UUID idDesafio,

        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "El codigo OTP debe tener exactamente 6 digitos numericos")
        String otp
) {
}
