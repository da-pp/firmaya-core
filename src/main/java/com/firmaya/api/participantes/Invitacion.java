package com.firmaya.api.participantes;

import com.firmaya.api.contratos.Contrato;
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

/** Mapea firmaya.Invitacion (CU-03). */
@Entity
@Table(name = "Invitacion", schema = "firmaya")
public class Invitacion {

    @Id
    @Column(name = "id_invitacion")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_emisor", nullable = false)
    private Usuario usuarioEmisor;

    @Column(name = "mensaje_personalizado", length = 500)
    private String mensajePersonalizado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private EstadoInvitacion estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_envio")
    private OffsetDateTime fechaUltimoEnvio;

    @Column(name = "fecha_aceptacion")
    private OffsetDateTime fechaAceptacion;

    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    protected Invitacion() {
    }

    public static Invitacion crear(UUID id, Contrato contrato, Participante participante, Usuario usuarioEmisor,
                                    String mensajePersonalizado, OffsetDateTime ahora) {
        Invitacion invitacion = new Invitacion();
        invitacion.id = id;
        invitacion.contrato = contrato;
        invitacion.participante = participante;
        invitacion.usuarioEmisor = usuarioEmisor;
        invitacion.mensajePersonalizado = mensajePersonalizado;
        invitacion.estado = EstadoInvitacion.PENDIENTE;
        invitacion.fechaCreacion = ahora;
        return invitacion;
    }

    public UUID getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public Participante getParticipante() {
        return participante;
    }

    public String getMensajePersonalizado() {
        return mensajePersonalizado;
    }

    public EstadoInvitacion getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaUltimoEnvio() {
        return fechaUltimoEnvio;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void marcarEnviada(OffsetDateTime ahora, OffsetDateTime expiracion) {
        this.estado = EstadoInvitacion.ENVIADA;
        this.fechaUltimoEnvio = ahora;
        this.fechaExpiracion = expiracion;
    }

    public void marcarErrorEnvio(OffsetDateTime ahora) {
        this.estado = EstadoInvitacion.ERROR_ENVIO;
        this.fechaUltimoEnvio = ahora;
    }

    public void marcarAceptada(OffsetDateTime ahora) {
        this.estado = EstadoInvitacion.ACEPTADA;
        this.fechaAceptacion = ahora;
    }
}
