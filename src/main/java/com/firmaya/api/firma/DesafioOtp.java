package com.firmaya.api.firma;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea firmaya.DesafioOtp. verificador_otp es una derivacion protegida con secreto del
 * servidor (HMAC-SHA256), nunca un hash simple del numero de 6 digitos ni el OTP en claro.
 */
@Entity
@Table(name = "DesafioOtp", schema = "firmaya")
public class DesafioOtp {

    @Id
    @Column(name = "id_desafio_otp")
    private UUID id;

    @Column(name = "id_solicitud_firma", nullable = false)
    private UUID idSolicitudFirma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aceptacion", nullable = false)
    private AceptacionFirma aceptacion;

    @Column(name = "verificador_otp", nullable = false)
    private byte[] verificadorOtp;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_consumo")
    private OffsetDateTime fechaConsumo;

    @Column(name = "fecha_invalidacion")
    private OffsetDateTime fechaInvalidacion;

    @Column(name = "fecha_bloqueo")
    private OffsetDateTime fechaBloqueo;

    protected DesafioOtp() {
    }

    public static DesafioOtp crear(UUID id, UUID idSolicitudFirma, AceptacionFirma aceptacion,
                                    byte[] verificadorOtp, OffsetDateTime ahora, OffsetDateTime expiracion) {
        DesafioOtp desafio = new DesafioOtp();
        desafio.id = id;
        desafio.idSolicitudFirma = idSolicitudFirma;
        desafio.aceptacion = aceptacion;
        desafio.verificadorOtp = verificadorOtp;
        desafio.intentosFallidos = 0;
        desafio.activo = true;
        desafio.fechaCreacion = ahora;
        desafio.fechaExpiracion = expiracion;
        return desafio;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdSolicitudFirma() {
        return idSolicitudFirma;
    }

    public AceptacionFirma getAceptacion() {
        return aceptacion;
    }

    public byte[] getVerificadorOtp() {
        return verificadorOtp;
    }

    public short getIntentosFallidos() {
        return intentosFallidos;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return activo && fechaExpiracion.isAfter(ahora);
    }

    public boolean estaBloqueado() {
        return intentosFallidos >= 3;
    }

    public void registrarIntentoFallido(OffsetDateTime ahora) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 3) {
            this.activo = false;
            this.fechaBloqueo = ahora;
        }
    }

    public void consumir(OffsetDateTime ahora) {
        this.activo = false;
        this.fechaConsumo = ahora;
    }

    public void invalidar(OffsetDateTime ahora) {
        this.activo = false;
        this.fechaInvalidacion = ahora;
    }
}
