package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.contratos.EstadoContrato;
import java.util.List;

public record OpcionesTransicionDto(
        EstadoContrato estadoActual,
        List<OpcionTransicionDto> permitidas
) {
}
