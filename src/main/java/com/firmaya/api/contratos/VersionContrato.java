package com.firmaya.api.contratos;

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

/** Mapea firmaya.VersionContrato. Fila inmutable (protegida por trigger en base de datos). */
@Entity
@Table(name = "VersionContrato", schema = "firmaya")
public class VersionContrato {

    @Id
    @Column(name = "id_version_contrato")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario usuarioAutor;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "comentario_version", length = 500)
    private String comentarioVersion;

    @Column(name = "id_version_restaurada")
    private UUID idVersionRestaurada;

    protected VersionContrato() {
    }

    public static VersionContrato crear(UUID id, Contrato contrato, int numeroVersion, String contenido,
                                         String hashSha256, Usuario usuarioAutor, OffsetDateTime ahora,
                                         String comentarioVersion, UUID idVersionRestaurada) {
        VersionContrato version = new VersionContrato();
        version.id = id;
        version.contrato = contrato;
        version.numeroVersion = numeroVersion;
        version.contenido = contenido;
        version.hashSha256 = hashSha256;
        version.usuarioAutor = usuarioAutor;
        version.fechaCreacion = ahora;
        version.comentarioVersion = comentarioVersion;
        version.idVersionRestaurada = idVersionRestaurada;
        return version;
    }

    public UUID getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public int getNumeroVersion() {
        return numeroVersion;
    }

    public String getContenido() {
        return contenido;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public Usuario getUsuarioAutor() {
        return usuarioAutor;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getComentarioVersion() {
        return comentarioVersion;
    }

    public UUID getIdVersionRestaurada() {
        return idVersionRestaurada;
    }
}
