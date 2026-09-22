package com.firmaya.api.panelactividad;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.panelactividad.dto.ConsultaContratosPanel;
import com.firmaya.api.panelactividad.dto.FilaContratoPanelDto;
import com.firmaya.api.panelactividad.dto.PanelActividadDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-17, EP-64..EP-66. Solo Administrador (ver SeguridadConfig: /api/v1/administracion/**
 * exige ROLE_ADMINISTRADOR).
 */
@RestController
@RequestMapping("/api/v1/administracion/panel-actividad")
public class ControllerPanelActividad {

    private final ServicePanelActividad servicePanelActividad;
    private final ServiceExportacionPanel serviceExportacionPanel;

    public ControllerPanelActividad(ServicePanelActividad servicePanelActividad,
                                      ServiceExportacionPanel serviceExportacionPanel) {
        this.servicePanelActividad = servicePanelActividad;
        this.serviceExportacionPanel = serviceExportacionPanel;
    }

    @GetMapping
    public PanelActividadDto obtenerResumen(@RequestParam(required = false) LocalDate origen,
                                             @RequestParam(required = false) LocalDate destino,
                                             @RequestParam(required = false) List<EstadoContrato> estados) {
        return servicePanelActividad.obtenerResumen(origen, destino, estados);
    }

    @GetMapping("/contratos")
    public PaginaDto<FilaContratoPanelDto> listarContratos(@RequestParam MetricaPanel metrica,
                                                             @RequestParam(required = false) LocalDate origen,
                                                             @RequestParam(required = false) LocalDate destino,
                                                             @RequestParam(required = false) List<EstadoContrato> estados,
                                                             @RequestParam(required = false) Integer pagina) {
        return servicePanelActividad.listarContratosPorMetrica(
                new ConsultaContratosPanel(metrica, origen, destino, estados, pagina));
    }

    @GetMapping("/contratos/exportar")
    public ResponseEntity<byte[]> exportarContratos(@RequestParam MetricaPanel metrica,
                                                      @RequestParam(required = false) LocalDate origen,
                                                      @RequestParam(required = false) LocalDate destino,
                                                      @RequestParam(required = false) List<EstadoContrato> estados) {
        byte[] csv = serviceExportacionPanel.exportarContratos(
                new ConsultaContratosPanel(metrica, origen, destino, estados, null));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"panel_actividad_firmaya.csv\"")
                .body(csv);
    }
}
