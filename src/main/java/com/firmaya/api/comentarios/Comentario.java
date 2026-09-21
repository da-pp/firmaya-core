package com.firmaya.api.comentarios;

import com.firmaya.api.contratos.VersionContrato;
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

/** Mapea firmaya.Comentario (CU-06). Autor: exactamente uno de usuarioAutor o idParticipanteAutor. */
@Entity
@Table(name = "Comentario", schema = "firmaya")
public class Comentario {

    @Id
    @Column(name = "id_comentario")
    private UUID id;

    @Column(name = "id_contrato", nullable = false)
    private UUID idContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_version_contrato", nullable = false)
    private VersionContrato versionContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_autor")
    private Usuario usuarioAutor;

    @Column(name = "id_participante_autor")
    private UUID idParticipanteAutor;

    @Column(name = "texto", nullable = false, length = 1000)
    private String texto;

    @Column(name = "texto_seleccionado")
    private String textoSeleccionado;

    @Column(name = "referencia_fragmento_json")
    private String referenciaFragmentoJson;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    protected Comentario() {
    }

    public static Comentario deUsuario(UUID id, VersionContrato version, Usuario autor, String texto,
                                        String textoSeleccionado, String referenciaFragmentoJson,
                                        OffsetDateTime ahora) {
        Comentario comentario = base(id, version, texto, textoSeleccionado, referenciaFragmentoJson, ahora);
        comentario.usuarioAutor = autor;
        return comentario;
    }

    public static Comentario deParticipante(UUID id, VersionContrato version, UUID idParticipante, String texto,
                                             String textoSeleccionado, String referenciaFragmentoJson,
                                             OffsetDateTime ahora) {
        Comentario comentario = base(id, version, texto, textoSeleccionado, referenciaFragmentoJson, ahora);
        comentario.idParticipanteAutor = idParticipante;
        return comentario;
    }

    private static Comentario base(UUID id, VersionContrato version, String texto, String textoSeleccionado,
                                    String referenciaFragmentoJson, OffsetDateTime ahora) {
        Comentario comentario = new Comentario();
        comentario.id = id;
        comentario.idContrato = version.getContrato().getId();
        comentario.versionContrato = version;
        comentario.texto = texto;
        comentario.textoSeleccionado = textoSeleccionado;
        comentario.referenciaFragmentoJson = referenciaFragmentoJson;
        comentario.fechaCreacion = ahora;
        return comentario;
    }

    public UUID getId() {
        return id;
    }

    public VersionContrato getVersionContrato() {
        return versionContrato;
    }

    public Usuario getUsuarioAutor() {
        return usuarioAutor;
    }

    public UUID getIdParticipanteAutor() {
        return idParticipanteAutor;
    }

    public String getTexto() {
        return texto;
    }

    public String getTextoSeleccionado() {
        return textoSeleccionado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
