package com.firmaya.api.panelactividad;

import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.panelactividad.dto.ConsultaContratosPanel;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-17, EP-66: exportacion CSV del desglose de una metrica, con los mismos filtros que EP-65. */
@Service
@Transactional(readOnly = true)
public class ServicioExportacionPanel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<String> ENCABEZADOS = List.of(
            "id_contrato", "nombre", "estado", "responsable", "fecha_actualizacion");

    private final ServicioPanelActividad servicioPanelActividad;

    public ServicioExportacionPanel(ServicioPanelActividad servicioPanelActividad) {
        this.servicioPanelActividad = servicioPanelActividad;
    }

    public byte[] exportarContratos(ConsultaContratosPanel consulta) {
        List<Contrato> contratos = servicioPanelActividad.listarTodosPorMetrica(consulta);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PrintWriter escritor = new PrintWriter(salida, false, StandardCharsets.UTF_8)) {
            escritor.println(String.join(",", ENCABEZADOS));
            for (Contrato contrato : contratos) {
                escritor.println(String.join(",",
                        celda(contrato.getId().toString()),
                        celda(contrato.getNombre()),
                        celda(contrato.getEstado().name()),
                        celda(contrato.getResponsable().getNombre() + " " + contrato.getResponsable().getApellido()),
                        celda(contrato.getFechaUltimaActividad().format(FORMATO_FECHA))));
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
}
