package com.firmaya.api.documentofinal;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.RepositoryVersionContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.documentofinal.dto.ArchivoDescargaDto;
import com.firmaya.api.documentofinal.dto.DocumentoFinalDto;
import com.firmaya.api.firma.Firma;
import com.firmaya.api.firma.RepositoryFirma;
import com.firmaya.api.procesofirma.ProcesoFirma;
import com.firmaya.api.procesofirma.RepositoryProcesoFirma;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-10, EP-56..EP-58. Generacion sincrona (sin cola/tarea diferida): esta implementacion
 * responde siempre 200/201 y nunca 202, decision tecnica [DISEÑO] documentada aqui, valida
 * porque el volumen de firmantes de un contrato es acotado. Idempotencia: un mismo contrato
 * firmado tiene una unica fila DocumentoFinal (UQ_DocumentoFinal_Contrato); reintentar la
 * generacion tras un error reutiliza esa fila en vez de crear una nueva.
 */
@Service
@Transactional
public class ServiceDocumentoFinal {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirma repositoryFirma;
    private final RepositoryDocumentoFinal repositoryDocumentoFinal;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceGeneracionPdf serviceGeneracionPdf;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final Path directorioAlmacenamiento;

    public ServiceDocumentoFinal(RepositoryContrato repositoryContrato,
                                   RepositoryVersionContrato repositoryVersionContrato,
                                   RepositoryProcesoFirma repositoryProcesoFirma,
                                   RepositoryFirma repositoryFirma,
                                   RepositoryDocumentoFinal repositoryDocumentoFinal,
                                   ServiceAutorizacionContrato serviceAutorizacionContrato,
                                   ServiceGeneracionPdf serviceGeneracionPdf,
                                   ServiceRegistroAuditoria serviceRegistroAuditoria,
                                   @Value("${firmaya.documentos.directorio-almacenamiento}") String directorioAlmacenamiento) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirma = repositoryFirma;
        this.repositoryDocumentoFinal = repositoryDocumentoFinal;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceGeneracionPdf = serviceGeneracionPdf;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.directorioAlmacenamiento = Path.of(directorioAlmacenamiento);
    }

    @Transactional(readOnly = true)
    public DocumentoFinalDto obtenerMetadatosParaUsuarioInterno(UUID idContrato, UUID idUsuario) {
        Contrato contrato = obtenerContratoFirmadoOLanzar(idContrato);
        serviceAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return obtenerMetadatos(idContrato);
    }

    @Transactional(readOnly = true)
    public DocumentoFinalDto obtenerMetadatosParaParticipanteExterno(UUID idContrato,
                                                                        ContextoParticipanteAutenticado contexto) {
        verificarAccesoConsultaExterna(idContrato, contexto);
        return obtenerMetadatos(idContrato);
    }

    private DocumentoFinalDto obtenerMetadatos(UUID idContrato) {
        DocumentoFinal documento = repositoryDocumentoFinal.findByContratoId(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El documento final aun no fue generado."));
        return DocumentoFinalDto.desde(documento);
    }

    public ResultadoGeneracionDocumentoFinal generarIdempotentemente(UUID idContrato, UUID idUsuario) {
        Contrato contrato = obtenerContratoFirmadoOLanzar(idContrato);
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        DocumentoFinal documento = repositoryDocumentoFinal.findByContratoId(idContrato).orElse(null);
        if (documento != null && documento.getEstado() == EstadoDocumentoFinal.DISPONIBLE) {
            return new ResultadoGeneracionDocumentoFinal(DocumentoFinalDto.desde(documento), false);
        }

        ProcesoFirma proceso = repositoryProcesoFirma.findTopByContratoIdOrderByFechaCreacionDesc(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no tiene un proceso de firma."));
        VersionContrato version = repositoryVersionContrato.findByIdAndContratoId(proceso.getIdVersionObjetivo(), idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version firmada no existe."));
        List<Firma> firmas = repositoryFirma.findByProcesoFirmaId(proceso.getId());
        if (firmas.isEmpty()) {
            throw new ConflictoEstadoException("El proceso de firma no tiene firmas registradas.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        if (documento == null) {
            documento = DocumentoFinal.crear(UUID.randomUUID(), contrato, proceso.getId(), version.getId(),
                    version.getHashSha256(), ahora);
            // Se reasigna save(): con @Id manual, save() usa merge() y la instancia original
            // queda desconectada; las mutaciones posteriores (marcarDisponible/marcarError)
            // deben aplicarse sobre la copia gestionada.
            documento = repositoryDocumentoFinal.save(documento);
        }

        try {
            byte[] bytesPdf = serviceGeneracionPdf.generar(contrato, version, firmas);
            String hashPdf = calcularHash(bytesPdf);
            String nombreArchivo = normalizarNombreArchivo(contrato.getNombre()) + "_v" + version.getNumeroVersion()
                    + "_firmado.pdf";
            Path rutaArchivo = escribirArchivo(idContrato, nombreArchivo, bytesPdf);

            documento.marcarDisponible(hashPdf, nombreArchivo, rutaArchivo.toString(), bytesPdf.length, ahora);

            serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deUsuario(contrato.getResponsable(), "DOCUMENTO_FINAL_GENERADO", "DocumentoFinal",
                            "PDF final generado y conservado.")
                    .conEntidad(documento.getId())
                    .conContrato(idContrato)
                    .conHash(hashPdf));

            return new ResultadoGeneracionDocumentoFinal(DocumentoFinalDto.desde(documento), true);
        } catch (RuntimeException | IOException ex) {
            documento.marcarError(ex.getMessage());
            throw new IllegalStateException("No se pudo generar el PDF final: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public ArchivoDescargaDto descargarParaUsuarioInterno(UUID idContrato, UUID idUsuario) {
        Contrato contrato = obtenerContratoFirmadoOLanzar(idContrato);
        serviceAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return descargar(idContrato);
    }

    @Transactional(readOnly = true)
    public ArchivoDescargaDto descargarParaParticipanteExterno(UUID idContrato, ContextoParticipanteAutenticado contexto) {
        verificarAccesoConsultaExterna(idContrato, contexto);
        return descargar(idContrato);
    }

    private ArchivoDescargaDto descargar(UUID idContrato) {
        DocumentoFinal documento = repositoryDocumentoFinal.findByContratoId(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El documento final aun no fue generado."));
        if (documento.getEstado() != EstadoDocumentoFinal.DISPONIBLE) {
            throw new ConflictoEstadoException("El documento final todavia no esta disponible para descargar.");
        }
        try {
            byte[] contenido = Files.readAllBytes(Path.of(documento.getUbicacionPrivada()));
            serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deSistema("DOCUMENTO_FINAL_DESCARGADO", "DocumentoFinal", "Descarga del PDF final solicitada.")
                    .conEntidad(documento.getId())
                    .conContrato(idContrato));
            return new ArchivoDescargaDto(contenido, documento.getNombreArchivo());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo final conservado.", ex);
        }
    }

    private void verificarAccesoConsultaExterna(UUID idContrato, ContextoParticipanteAutenticado contexto) {
        if (!contexto.idContrato().equals(idContrato)) {
            throw new AccesoDenegadoNegocioException("Su acceso no corresponde a este contrato.");
        }
        if (contexto.proposito() != PropositoAcceso.CONSULTA) {
            throw new AccesoDenegadoNegocioException("Su enlace no habilita la consulta del documento final.");
        }
    }

    private Contrato obtenerContratoFirmadoOLanzar(UUID idContrato) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        if (contrato.getEstado() != EstadoContrato.FIRMADO && contrato.getEstado() != EstadoContrato.ARCHIVADO) {
            throw new ConflictoEstadoException("El documento final solo esta disponible para contratos Firmados o Archivados.");
        }
        return contrato;
    }

    private Path escribirArchivo(UUID idContrato, String nombreArchivo, byte[] bytesPdf) throws IOException {
        Path directorioContrato = directorioAlmacenamiento.resolve(idContrato.toString());
        Files.createDirectories(directorioContrato);
        Path ruta = directorioContrato.resolve(nombreArchivo);
        Files.write(ruta, bytesPdf);
        return ruta;
    }

    private String normalizarNombreArchivo(String nombre) {
        String sinAcentos = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalizado = sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        return normalizado.replaceAll("^_+|_+$", "");
    }

    private String calcularHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", ex);
        }
    }

    public record ResultadoGeneracionDocumentoFinal(DocumentoFinalDto documento, boolean recienGenerado) {
    }
}
