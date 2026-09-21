package com.firmaya.api.panelactividad.dto;

import com.firmaya.api.contratos.EstadoContrato;

public record ConteoPorEstadoDto(EstadoContrato estado, long cantidad) {
}
