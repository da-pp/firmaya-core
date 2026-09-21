package com.firmaya.api.contratos;

import com.firmaya.api.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.Contrato (CU-01, CU-02, CU-04, CU-05, CU-13). */
@Entity
@Table(name = "Contrato", schema = "firmaya")
public class Contrato {

    @Id
    @Column(name = "id_contrato")
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 15)
    private TipoContrato tipoContrato;

    @Column(name = "partes_involucradas", nullable = false, length = 1000)
    private String partesInvolucradas;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "descripcion_propiedad", length = 2000)
    private String descripcionPropiedad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable", nullable = false)
    private Usuario responsable;

    @Column(name = "id_plantilla", nullable = false)
    private UUID idPlantilla;

    @Column(name = "id_version_plantilla", nullable = false)
    private UUID idVersionPlantilla;

    @Column(name = "id_version_actual")
    private UUID idVersionActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 19)
    private EstadoContrato estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_ultima_actividad", nullable = false)
    private OffsetDateTime fechaUltimaActividad;

    @Column(name = "fecha_firma")
    private OffsetDateTime fechaFirma;

    @Column(name = "fecha_archivado")
    private OffsetDateTime fechaArchivado;

    protected Contrato() {
    }

    public static Contrato crear(UUID id, String nombre, TipoContrato tipoContrato, String partesInvolucradas,
                                  LocalDate fechaInicio, LocalDate fechaExpiracion, String descripcionPropiedad,
                                  Usuario responsable, UUID idPlantilla, UUID idVersionPlantilla,
                                  OffsetDateTime ahora) {
        Contrato contrato = new Contrato();
        contrato.id = id;
        contrato.nombre = nombre;
        contrato.tipoContrato = tipoContrato;
        contrato.partesInvolucradas = partesInvolucradas;
        contrato.fechaInicio = fechaInicio;
        contrato.fechaExpiracion = fechaExpiracion;
        contrato.descripcionPropiedad = descripcionPropiedad;
        contrato.responsable = responsable;
        contrato.idPlantilla = idPlantilla;
        contrato.idVersionPlantilla = idVersionPlantilla;
        contrato.estado = EstadoContrato.BORRADOR;
        contrato.fechaCreacion = ahora;
        contrato.fechaUltimaActividad = ahora;
        return contrato;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public String getPartesInvolucradas() {
        return partesInvolucradas;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaExpiracion() {
        return fechaExpiracion;
    }

    public String getDescripcionPropiedad() {
        return descripcionPropiedad;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public UUID getIdPlantilla() {
        return idPlantilla;
    }

    public UUID getIdVersionPlantilla() {
        return idVersionPlantilla;
    }

    public UUID getIdVersionActual() {
        return idVersionActual;
    }

    public EstadoContrato getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaUltimaActividad() {
        return fechaUltimaActividad;
    }

    public OffsetDateTime getFechaFirma() {
        return fechaFirma;
    }

    public OffsetDateTime getFechaArchivado() {
        return fechaArchivado;
    }

    public boolean esResponsable(UUID idUsuario) {
        return responsable.getId().equals(idUsuario);
    }

    public boolean esEditable() {
        return estado == EstadoContrato.BORRADOR || estado == EstadoContrato.EN_REVISION;
    }

    public void establecerVersionActual(UUID idVersion, OffsetDateTime ahora) {
        this.idVersionActual = idVersion;
        this.fechaUltimaActividad = ahora;
    }

    public void cambiarEstado(EstadoContrato nuevoEstado, OffsetDateTime ahora) {
        this.estado = nuevoEstado;
        this.fechaUltimaActividad = ahora;
        if (nuevoEstado == EstadoContrato.FIRMADO) {
            this.fechaFirma = ahora;
        }
        if (nuevoEstado == EstadoContrato.ARCHIVADO) {
            this.fechaArchivado = ahora;
        }
    }
}
