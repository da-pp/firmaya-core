package com.firmaya.api.firma.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudAceptacionFirma(
        @NotNull UUID idSolicitud,
        @NotNull UUID idVersion,

        @NotBlank(message = "El hash es obligatorio")
        String hashSha256,

        @AssertTrue(message = "Debe aceptar expresamente el contenido para continuar")
        boolean aceptado
) {
}
