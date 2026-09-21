package com.firmaya.api.procesofirma.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SolicitudRenovarSolicitudFirma(
        @Future(message = "La nueva fecha limite debe ser posterior a la fecha actual")
        LocalDate fechaLimiteNueva,

        @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
        String mensaje
) {
}
