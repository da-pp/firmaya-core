package com.firmaya.api.documentofinal;

import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.firma.Firma;
import com.firmaya.api.procesofirma.FirmanteProceso;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

/**
 * CU-10 (EP-57): construye los bytes del PDF final servicio interno transversal, no expuesto
 * como endpoint. Usa exactamente el contenido de la version congelada que fue firmada; incluye
 * una seccion de evidencias de firma electronica por firmante (nombre, fecha/hora, IP, hash de
 * la version) sin OTP ni otros secretos, y una leyenda que no afirma certificacion de firma
 * digital (ver CU-10, paso 21).
 */
@Component
public class ServicioGeneracionPdf {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEN = 50;
    private static final float TAMANO_FUENTE = 10;
    private static final float ALTURA_LINEA = 14;

    public byte[] generar(Contrato contrato, VersionContrato version, List<Firma> firmas) {
        try (PDDocument documento = new PDDocument()) {
            PDFont fuente = PDType1Font.HELVETICA;
            PDFont fuenteNegrita = PDType1Font.HELVETICA_BOLD;

            List<String> lineasContenido = envolverTexto(version.getContenido(), fuente, TAMANO_FUENTE,
                    PDRectangle.A4.getWidth() - 2 * MARGEN);

            EscritorPdf escritor = new EscritorPdf(documento, fuente, fuenteNegrita);
            escritor.escribirTitulo(contrato.getNombre());
            escritor.escribirLinea("Version firmada: v" + version.getNumeroVersion()
                    + " - Hash SHA-256: " + version.getHashSha256());
            escritor.espaciar();
            for (String linea : lineasContenido) {
                escritor.escribirLinea(linea);
            }

            escritor.espaciar();
            escritor.escribirSubtitulo("Evidencia de firma electronica");
            for (Firma firma : firmas) {
                FirmanteProceso firmante = firma.getFirmanteProceso();
                escritor.escribirLinea("Firmante: " + firmante.getNombreCongelado() + " <" + firmante.getCorreoCongelado() + ">");
                escritor.escribirLinea("Fecha y hora de firma: " + firma.getFechaFirma().format(FORMATO_FECHA));
                escritor.escribirLinea("Direccion IP: " + firma.getDireccionIp());
                escritor.escribirLinea("Hash de la version firmada: " + firma.getHashVersion());
                escritor.espaciar();
            }

            escritor.espaciar();
            escritor.escribirLinea("Firma electronica mediante mecanismo de aceptacion expresa y verificacion por "
                    + "codigo de un solo uso (OTP), enviado al correo electronico registrado de cada firmante. "
                    + "Este documento no constituye una firma digital certificada.");
            escritor.cerrar();

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF final.", ex);
        }
    }

    private List<String> envolverTexto(String texto, PDFont fuente, float tamanoFuente, float anchoDisponible) {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : texto.split("\n", -1)) {
            if (parrafo.isEmpty()) {
                lineas.add("");
                continue;
            }
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
                if (anchoTexto(candidato, fuente, tamanoFuente) > anchoDisponible && !actual.isEmpty()) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(candidato);
                }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private float anchoTexto(String texto, PDFont fuente, float tamanoFuente) {
        try {
            return fuente.getStringWidth(texto) / 1000 * tamanoFuente;
        } catch (IOException ex) {
            return texto.length() * tamanoFuente * 0.6f;
        }
    }

    /** Envuelve la paginacion manual de PDFBox (una pagina no tiene mas espacio que su alto menos margenes). */
    private static final class EscritorPdf implements AutoCloseable {
        private final PDDocument documento;
        private final PDFont fuente;
        private final PDFont fuenteNegrita;
        private PDPageContentStream flujo;
        private float posicionY;

        EscritorPdf(PDDocument documento, PDFont fuente, PDFont fuenteNegrita) throws IOException {
            this.documento = documento;
            this.fuente = fuente;
            this.fuenteNegrita = fuenteNegrita;
            nuevaPagina();
        }

        void escribirTitulo(String texto) throws IOException {
            escribirConFuente(texto, fuenteNegrita, TAMANO_FUENTE + 4);
        }

        void escribirSubtitulo(String texto) throws IOException {
            escribirConFuente(texto, fuenteNegrita, TAMANO_FUENTE + 1);
        }

        void escribirLinea(String texto) throws IOException {
            escribirConFuente(texto, fuente, TAMANO_FUENTE);
        }

        void espaciar() {
            posicionY -= ALTURA_LINEA / 2;
        }

        private void escribirConFuente(String texto, PDFont fuenteUsada, float tamano) throws IOException {
            if (posicionY < MARGEN) {
                nuevaPagina();
            }
            flujo.beginText();
            flujo.setFont(fuenteUsada, tamano);
            flujo.newLineAtOffset(MARGEN, posicionY);
            flujo.showText(sanear(texto));
            flujo.endText();
            posicionY -= ALTURA_LINEA;
        }

        private String sanear(String texto) {
            return texto.replaceAll("[\\r\\t]", " ");
        }

        private void nuevaPagina() throws IOException {
            if (flujo != null) {
                flujo.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            flujo = new PDPageContentStream(documento, pagina);
            posicionY = PDRectangle.A4.getHeight() - MARGEN;
        }

        @Override
        public void close() throws IOException {
            flujo.close();
        }

        void cerrar() throws IOException {
            close();
        }
    }
}
