package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.contratos.EstadoContrato;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCambiarEstadoContrato(
        @NotNull(message = "El estado destino es obligatorio")
        EstadoContrato estadoDestino,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo,

        @NotNull(message = "El estado actual esperado es obligatorio")
        EstadoContrato estadoActualEsperado
) {
}
