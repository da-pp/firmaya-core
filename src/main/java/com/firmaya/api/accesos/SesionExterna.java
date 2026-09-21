package com.firmaya.api.accesos;

import com.firmaya.api.participantes.Participante;
import com.firmaya.api.tokens.TokenAcceso;
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

/** Mapea firmaya.SesionExterna (CU-04): sesion acotada a un participante, contrato y proposito. */
@Entity
@Table(name = "SesionExterna", schema = "firmaya")
public class SesionExterna {

    @Id
    @Column(name = "id_sesion_externa")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_token_origen", nullable = false)
    private TokenAcceso tokenOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    @Column(name = "hash_credencial", nullable = false)
    private byte[] hashCredencial;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposito", nullable = false, length = 10)
    private PropositoAcceso proposito;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_ultima_actividad", nullable = false)
    private OffsetDateTime fechaUltimaActividad;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_revocacion")
    private OffsetDateTime fechaRevocacion;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    protected SesionExterna() {
    }

    public static SesionExterna crear(UUID id, TokenAcceso tokenOrigen, Participante participante,
                                       byte[] hashCredencial, PropositoAcceso proposito, String direccionIp,
                                       OffsetDateTime ahora, OffsetDateTime expiracion) {
        SesionExterna sesion = new SesionExterna();
        sesion.id = id;
        sesion.tokenOrigen = tokenOrigen;
        sesion.participante = participante;
        sesion.hashCredencial = hashCredencial;
        sesion.proposito = proposito;
        sesion.direccionIp = direccionIp;
        sesion.fechaCreacion = ahora;
        sesion.fechaUltimaActividad = ahora;
        sesion.fechaExpiracion = expiracion;
        return sesion;
    }

    public UUID getId() {
        return id;
    }

    public TokenAcceso getTokenOrigen() {
        return tokenOrigen;
    }

    public Participante getParticipante() {
        return participante;
    }

    public PropositoAcceso getProposito() {
        return proposito;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return fechaRevocacion == null && fechaExpiracion.isAfter(ahora);
    }

    public void revocar(OffsetDateTime ahora) {
        this.fechaRevocacion = ahora;
    }

    public void extender(OffsetDateTime ahora, OffsetDateTime nuevaExpiracion) {
        this.fechaUltimaActividad = ahora;
        this.fechaExpiracion = nuevaExpiracion;
    }
}
