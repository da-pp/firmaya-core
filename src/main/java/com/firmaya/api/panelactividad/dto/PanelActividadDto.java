package com.firmaya.api.panelactividad.dto;

import com.firmaya.api.contratos.dto.ResumenContratoDto;
import java.time.LocalDate;
import java.util.List;

public record PanelActividadDto(
        long contratosActivos,
        long contratosPendientesFirma,
        long firmadosEnPeriodo,
        long proximosAVencerEn7Dias,
        List<ConteoPorEstadoDto> porEstado,
        List<ResumenContratoDto> contratosRecientes,
        FiltrosAplicadosDto filtrosAplicados
) {
    public record FiltrosAplicadosDto(LocalDate origen, LocalDate destino, List<String> estados) {
    }
}
