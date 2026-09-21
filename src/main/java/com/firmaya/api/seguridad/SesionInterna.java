package com.firmaya.api.seguridad;

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

/** Mapea firmaya.SesionInterna: credencial de sesion propia (no HttpSession de servlet). */
@Entity
@Table(name = "SesionInterna", schema = "firmaya")
public class SesionInterna {

    @Id
    @Column(name = "id_sesion_interna")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "hash_credencial", nullable = false)
    private byte[] hashCredencial;

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

    @Column(name = "agente_usuario", length = 512)
    private String agenteUsuario;

    protected SesionInterna() {
    }

    public SesionInterna(UUID id, Usuario usuario, byte[] hashCredencial, OffsetDateTime fechaCreacion,
                          OffsetDateTime fechaExpiracion, String direccionIp, String agenteUsuario) {
        this.id = id;
        this.usuario = usuario;
        this.hashCredencial = hashCredencial;
        this.fechaCreacion = fechaCreacion;
        this.fechaUltimaActividad = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.direccionIp = direccionIp;
        this.agenteUsuario = agenteUsuario;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public byte[] getHashCredencial() {
        return hashCredencial;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public OffsetDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return fechaRevocacion == null && fechaExpiracion.isAfter(ahora);
    }

    public void revocar(OffsetDateTime ahora) {
        this.fechaRevocacion = ahora;
    }

    public void registrarActividad(OffsetDateTime ahora) {
        this.fechaUltimaActividad = ahora;
    }
}
