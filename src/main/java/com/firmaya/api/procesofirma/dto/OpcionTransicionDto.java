package com.firmaya.api.procesofirma.dto;

import com.firmaya.api.contratos.EstadoContrato;

public record OpcionTransicionDto(
        EstadoContrato estadoDestino,
        String explicacion,
        boolean precondicionesCumplidas,
        String motivosBloqueo
) {
}
