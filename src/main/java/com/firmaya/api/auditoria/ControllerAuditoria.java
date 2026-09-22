package com.firmaya.api.auditoria;

import com.firmaya.api.auditoria.dto.ConsultaAuditoriaDto;
import com.firmaya.api.auditoria.dto.DetalleEventoAuditoriaDto;
import com.firmaya.api.auditoria.dto.EventoAuditoriaDto;
import com.firmaya.api.comun.PaginaDto;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-18, EP-67..EP-69. Solo Administrador (ver SeguridadConfig: /api/v1/administracion/**
 * exige ROLE_ADMINISTRADOR). La escritura de eventos es un service interno
 * (ServiceRegistroAuditoria); este controller es exclusivamente de lectura/exportacion.
 */
@RestController
@RequestMapping("/api/v1/administracion/eventos-auditoria")
public class ControllerAuditoria {

    private final ServiceConsultaAuditoria serviceConsultaAuditoria;
    private final ServiceExportacionAuditoria serviceExportacionAuditoria;

    public ControllerAuditoria(ServiceConsultaAuditoria serviceConsultaAuditoria,
                                 ServiceExportacionAuditoria serviceExportacionAuditoria) {
        this.serviceConsultaAuditoria = serviceConsultaAuditoria;
        this.serviceExportacionAuditoria = serviceExportacionAuditoria;
    }

    @GetMapping
    public PaginaDto<EventoAuditoriaDto> buscar(@RequestParam(required = false) LocalDate origen,
                                                 @RequestParam(required = false) LocalDate destino,
                                                 @RequestParam(required = false) UUID idUsuario,
                                                 @RequestParam(required = false) String tipoAccion,
                                                 @RequestParam(required = false) UUID idContrato,
                                                 @RequestParam(required = false) Integer pagina) {
        return serviceConsultaAuditoria.buscar(
                new ConsultaAuditoriaDto(origen, destino, idUsuario, tipoAccion, idContrato, pagina));
    }

    @GetMapping("/{idEvento}")
    public DetalleEventoAuditoriaDto obtenerDetalle(@PathVariable UUID idEvento) {
        return serviceConsultaAuditoria.obtenerDetalle(idEvento);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) LocalDate origen,
                                            @RequestParam(required = false) LocalDate destino,
                                            @RequestParam(required = false) UUID idUsuario,
                                            @RequestParam(required = false) String tipoAccion,
                                            @RequestParam(required = false) UUID idContrato) {
        byte[] csv = serviceExportacionAuditoria.exportar(
                new ConsultaAuditoriaDto(origen, destino, idUsuario, tipoAccion, idContrato, null));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria_firmaya.csv\"")
                .body(csv);
    }
}
