package com.firmaya.api.panelactividad.dto;

import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.panelactividad.MetricaPanel;
import java.time.LocalDate;
import java.util.List;

public record ConsultaContratosPanel(
        MetricaPanel metrica,
        LocalDate origen,
        LocalDate destino,
        List<EstadoContrato> estados,
        Integer pagina
) {
}
