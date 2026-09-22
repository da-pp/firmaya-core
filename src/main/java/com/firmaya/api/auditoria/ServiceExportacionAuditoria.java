package com.firmaya.api.auditoria;

import com.firmaya.api.auditoria.dto.ConsultaAuditoriaDto;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exportacion CSV de auditoria (EP-69). Sanea secretos (nunca aparecen en EventoAuditoria,
 * ver ServiceRegistroAuditoria), evita inyeccion de formulas CSV y respeta los mismos
 * filtros que la consulta paginada.
 */
@Service
@Transactional(readOnly = true)
public class ServiceExportacionAuditoria {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<String> ENCABEZADOS = List.of(
            "fecha_evento", "actor", "tipo_accion", "tipo_entidad", "id_entidad", "descripcion", "ip");

    private final RepositoryEventoAuditoria repositoryEventoAuditoria;
    private final ServiceConsultaAuditoria serviceConsultaAuditoria;

    public ServiceExportacionAuditoria(RepositoryEventoAuditoria repositoryEventoAuditoria,
                                         ServiceConsultaAuditoria serviceConsultaAuditoria) {
        this.repositoryEventoAuditoria = repositoryEventoAuditoria;
        this.serviceConsultaAuditoria = serviceConsultaAuditoria;
    }

    public byte[] exportar(ConsultaAuditoriaDto consulta) {
        var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "fechaEvento"));
        var eventos = repositoryEventoAuditoria.buscar(
                inicioDeDia(consulta.origen()),
                finDeDia(consulta.destino()),
                consulta.idUsuario(),
                consulta.tipoAccion(),
                consulta.idContrato(),
                pageable).getContent();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PrintWriter escritor = new PrintWriter(salida, false, StandardCharsets.UTF_8)) {
            escritor.println(String.join(",", ENCABEZADOS));
            for (var dto : eventos.stream().map(serviceConsultaAuditoria::aResumenPublico).toList()) {
                escritor.println(String.join(",",
                        celda(dto.fechaEvento() != null ? dto.fechaEvento().format(FORMATO_FECHA) : ""),
                        celda(dto.actor()),
                        celda(dto.tipoAccion()),
                        celda(dto.tipoEntidad()),
                        celda(dto.idEntidad() != null ? dto.idEntidad().toString() : ""),
                        celda(dto.descripcion()),
                        celda(dto.ipEnmascarada())));
            }
        }
        return salida.toByteArray();
    }

    private String celda(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        String escapado = texto.replace("\"", "\"\"");
        return "\"" + escapado + "\"";
    }

    private java.time.OffsetDateTime inicioDeDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }

    private java.time.OffsetDateTime finDeDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}
