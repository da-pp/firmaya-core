package com.firmaya.api.documentofinal;

import com.firmaya.api.contratos.Contrato;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea firmaya.DocumentoFinal (CU-10). El hash del PDF se calcula y registra DESPUES de
 * cerrar todos sus bytes (CK_DocumentoFinal_Disponible exige hash_pdf/nombre/ubicacion/tamano
 * juntos solo cuando estado = DISPONIBLE). ubicacion_privada nunca se expone en la API.
 */
@Entity
@Table(name = "DocumentoFinal", schema = "firmaya")
public class DocumentoFinal {

    @Id
    @Column(name = "id_documento_final")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    /** Columna cruda requerida por la FK compuesta (id_contrato, id_proceso_firma) hacia ProcesoFirma. */
    @Column(name = "id_proceso_firma", nullable = false)
    private UUID idProcesoFirma;

    @Column(name = "id_version_contrato", nullable = false)
    private UUID idVersionContrato;

    @Column(name = "hash_contenido", nullable = false, length = 64)
    private String hashContenido;

    @Column(name = "hash_pdf", length = 64)
    private String hashPdf;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "ubicacion_privada", length = 1024)
    private String ubicacionPrivada;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoDocumentoFinal estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_generacion")
    private OffsetDateTime fechaGeneracion;

    @Column(name = "detalle_error", length = 1000)
    private String detalleError;

    protected DocumentoFinal() {
    }

    public static DocumentoFinal crear(UUID id, Contrato contrato, UUID idProcesoFirma, UUID idVersionContrato,
                                        String hashContenido, OffsetDateTime ahora) {
        DocumentoFinal documento = new DocumentoFinal();
        documento.id = id;
        documento.contrato = contrato;
        documento.idProcesoFirma = idProcesoFirma;
        documento.idVersionContrato = idVersionContrato;
        documento.hashContenido = hashContenido;
        documento.estado = EstadoDocumentoFinal.GENERANDO;
        documento.fechaCreacion = ahora;
        return documento;
    }

    public void marcarDisponible(String hashPdf, String nombreArchivo, String ubicacionPrivada, long tamanoBytes,
                                  OffsetDateTime ahora) {
        this.hashPdf = hashPdf;
        this.nombreArchivo = nombreArchivo;
        this.ubicacionPrivada = ubicacionPrivada;
        this.tamanoBytes = tamanoBytes;
        this.estado = EstadoDocumentoFinal.DISPONIBLE;
        this.fechaGeneracion = ahora;
        this.detalleError = null;
    }

    public void marcarError(String detalleError) {
        this.estado = EstadoDocumentoFinal.ERROR;
        this.detalleError = detalleError;
    }

    public UUID getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public UUID getIdVersionContrato() {
        return idVersionContrato;
    }

    public String getHashContenido() {
        return hashContenido;
    }

    public String getHashPdf() {
        return hashPdf;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getUbicacionPrivada() {
        return ubicacionPrivada;
    }

    public EstadoDocumentoFinal getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }
}
