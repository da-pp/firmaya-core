package com.firmaya.api.tokens;

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

/**
 * Mapea firmaya.TokenAcceso. Cada fila usa exactamente una de las columnas de destino
 * (id_usuario, id_invitacion, id_solicitud_firma, id_participante) segun su proposito,
 * tal como exige CK_TokenAcceso_Destino; las fabricas estaticas garantizan esa exclusividad.
 */
@Entity
@Table(name = "TokenAcceso", schema = "firmaya")
public class TokenAcceso {

    @Id
    @Column(name = "id_token")
    private UUID id;

    @Column(name = "hash_token", nullable = false)
    private byte[] hashToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposito", nullable = false, length = 28)
    private PropositoToken proposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "id_invitacion")
    private UUID idInvitacion;

    @Column(name = "id_solicitud_firma")
    private UUID idSolicitudFirma;

    @Column(name = "id_participante")
    private UUID idParticipante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_emisor")
    private Usuario usuarioEmisor;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_consumo")
    private OffsetDateTime fechaConsumo;

    @Column(name = "fecha_revocacion")
    private OffsetDateTime fechaRevocacion;

    @Column(name = "motivo_revocacion", length = 300)
    private String motivoRevocacion;

    protected TokenAcceso() {
    }

    public static TokenAcceso deRecuperacionContrasena(UUID id, Usuario usuario, byte[] hashToken,
                                                         OffsetDateTime fechaEmision, OffsetDateTime fechaExpiracion) {
        TokenAcceso token = new TokenAcceso();
        token.id = id;
        token.hashToken = hashToken;
        token.proposito = PropositoToken.RECUPERACION_CONTRASENA;
        token.usuario = usuario;
        token.fechaEmision = fechaEmision;
        token.fechaExpiracion = fechaExpiracion;
        return token;
    }

    public static TokenAcceso deActivacion(UUID id, Usuario usuario, byte[] hashToken,
                                            OffsetDateTime fechaEmision, OffsetDateTime fechaExpiracion) {
        TokenAcceso token = new TokenAcceso();
        token.id = id;
        token.hashToken = hashToken;
        token.proposito = PropositoToken.ACTIVACION;
        token.usuario = usuario;
        token.fechaEmision = fechaEmision;
        token.fechaExpiracion = fechaExpiracion;
        return token;
    }

    public static TokenAcceso deInvitacion(UUID id, UUID idInvitacion, Usuario usuarioEmisor, byte[] hashToken,
                                            OffsetDateTime fechaEmision, OffsetDateTime fechaExpiracion) {
        TokenAcceso token = new TokenAcceso();
        token.id = id;
        token.hashToken = hashToken;
        token.proposito = PropositoToken.INVITACION;
        token.idInvitacion = idInvitacion;
        token.usuarioEmisor = usuarioEmisor;
        token.fechaEmision = fechaEmision;
        token.fechaExpiracion = fechaExpiracion;
        return token;
    }

    public static TokenAcceso deFirma(UUID id, UUID idSolicitudFirma, byte[] hashToken,
                                       OffsetDateTime fechaEmision, OffsetDateTime fechaExpiracion) {
        TokenAcceso token = new TokenAcceso();
        token.id = id;
        token.hashToken = hashToken;
        token.proposito = PropositoToken.FIRMA;
        token.idSolicitudFirma = idSolicitudFirma;
        token.fechaEmision = fechaEmision;
        token.fechaExpiracion = fechaExpiracion;
        return token;
    }

    public static TokenAcceso deConsulta(UUID id, UUID idParticipante, Usuario usuarioEmisor, byte[] hashToken,
                                          OffsetDateTime fechaEmision, OffsetDateTime fechaExpiracion) {
        TokenAcceso token = new TokenAcceso();
        token.id = id;
        token.hashToken = hashToken;
        token.proposito = PropositoToken.CONSULTA;
        token.idParticipante = idParticipante;
        token.usuarioEmisor = usuarioEmisor;
        token.fechaEmision = fechaEmision;
        token.fechaExpiracion = fechaExpiracion;
        return token;
    }

    public UUID getId() {
        return id;
    }

    public byte[] getHashToken() {
        return hashToken;
    }

    public PropositoToken getProposito() {
        return proposito;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public UUID getIdInvitacion() {
        return idInvitacion;
    }

    public UUID getIdSolicitudFirma() {
        return idSolicitudFirma;
    }

    public UUID getIdParticipante() {
        return idParticipante;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public OffsetDateTime getFechaConsumo() {
        return fechaConsumo;
    }

    public OffsetDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return fechaConsumo == null && fechaRevocacion == null && fechaExpiracion.isAfter(ahora);
    }

    public void consumir(OffsetDateTime ahora) {
        this.fechaConsumo = ahora;
    }

    public void revocar(OffsetDateTime ahora, String motivo) {
        this.fechaRevocacion = ahora;
        this.motivoRevocacion = motivo;
    }
}
