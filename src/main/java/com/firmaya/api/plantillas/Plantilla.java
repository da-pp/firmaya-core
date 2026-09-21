package com.firmaya.api.plantillas;

import com.firmaya.api.contratos.TipoContrato;
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
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.Plantilla (CU-01: solo lectura; CU-16: administracion/creacion). */
@Entity
@Table(name = "Plantilla", schema = "firmaya")
public class Plantilla {

    @Id
    @Column(name = "id_plantilla")
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 15)
    private TipoContrato tipoContrato;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 8)
    private EstadoPlantilla estado;

    @Column(name = "id_version_actual")
    private UUID idVersionActual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private Usuario usuarioCreador;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Plantilla() {
    }

    public static Plantilla crear(UUID id, String nombre, TipoContrato tipoContrato, String descripcion,
                                   EstadoPlantilla estado, Usuario usuarioCreador, OffsetDateTime ahora) {
        Plantilla plantilla = new Plantilla();
        plantilla.id = id;
        plantilla.nombre = nombre;
        plantilla.tipoContrato = tipoContrato;
        plantilla.descripcion = descripcion;
        plantilla.estado = estado;
        plantilla.usuarioCreador = usuarioCreador;
        plantilla.fechaCreacion = ahora;
        plantilla.fechaActualizacion = ahora;
        return plantilla;
    }

    public void establecerVersionActual(UUID idVersion, OffsetDateTime ahora) {
        this.idVersionActual = idVersion;
        this.fechaActualizacion = ahora;
    }

    public void cambiarEstado(EstadoPlantilla nuevoEstado, OffsetDateTime ahora) {
        this.estado = nuevoEstado;
        this.fechaActualizacion = ahora;
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

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoPlantilla getEstado() {
        return estado;
    }

    public UUID getIdVersionActual() {
        return idVersionActual;
    }

    public Usuario getUsuarioCreador() {
        return usuarioCreador;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
