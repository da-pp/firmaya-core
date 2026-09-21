package com.firmaya.api.procesofirma.dto;

import jakarta.validation.constraints.Size;

public record SolicitudCancelarProcesoFirma(
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
