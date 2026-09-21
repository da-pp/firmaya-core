package com.firmaya.api.plantillas;

import com.firmaya.api.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.VersionPlantilla. Fila inmutable (protegida por trigger en base de datos). */
@Entity
@Table(name = "VersionPlantilla", schema = "firmaya")
public class VersionPlantilla {

    @Id
    @Column(name = "id_version_plantilla")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla", nullable = false)
    private Plantilla plantilla;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "contenido", nullable = false)
    private String contenido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario usuarioAutor;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    protected VersionPlantilla() {
    }

    public static VersionPlantilla crear(UUID id, Plantilla plantilla, int numeroVersion, String contenido,
                                          Usuario usuarioAutor, OffsetDateTime ahora) {
        VersionPlantilla version = new VersionPlantilla();
        version.id = id;
        version.plantilla = plantilla;
        version.numeroVersion = numeroVersion;
        version.contenido = contenido;
        version.usuarioAutor = usuarioAutor;
        version.fechaCreacion = ahora;
        return version;
    }

    public UUID getId() {
        return id;
    }

    public Plantilla getPlantilla() {
        return plantilla;
    }

    public int getNumeroVersion() {
        return numeroVersion;
    }

    public String getContenido() {
        return contenido;
    }

    public Usuario getUsuarioAutor() {
        return usuarioAutor;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
