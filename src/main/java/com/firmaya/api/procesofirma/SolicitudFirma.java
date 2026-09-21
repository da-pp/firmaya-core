package com.firmaya.api.procesofirma;

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

/** Mapea firmaya.SolicitudFirma (CU-07/CU-08/CU-09). Canal unico soportado: correo electronico. */
@Entity
@Table(name = "SolicitudFirma", schema = "firmaya")
public class SolicitudFirma {

    @Id
    @Column(name = "id_solicitud_firma")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proceso_firma", nullable = false)
    private ProcesoFirma procesoFirma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_firmante_proceso", nullable = false)
    private FirmanteProceso firmanteProceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoSolicitudFirma estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 10)
    private EstadoEntregaSolicitud estadoEntrega;

    @Column(name = "mensaje_personalizado", length = 500)
    private String mensajePersonalizado;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "fecha_expiracion_enlace")
    private OffsetDateTime fechaExpiracionEnlace;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_envio")
    private OffsetDateTime fechaUltimoEnvio;

    @Column(name = "fecha_firma")
    private OffsetDateTime fechaFirma;

    @Column(name = "numero_renovaciones", nullable = false)
    private int numeroRenovaciones;

    protected SolicitudFirma() {
    }

    public static SolicitudFirma crear(UUID id, ProcesoFirma procesoFirma, FirmanteProceso firmanteProceso,
                                        String mensajePersonalizado, LocalDate fechaLimite, OffsetDateTime ahora) {
        SolicitudFirma solicitud = new SolicitudFirma();
        solicitud.id = id;
        solicitud.procesoFirma = procesoFirma;
        solicitud.firmanteProceso = firmanteProceso;
        solicitud.estado = EstadoSolicitudFirma.PENDIENTE;
        solicitud.estadoEntrega = EstadoEntregaSolicitud.PENDIENTE;
        solicitud.mensajePersonalizado = mensajePersonalizado;
        solicitud.fechaLimite = fechaLimite;
        solicitud.fechaCreacion = ahora;
        solicitud.numeroRenovaciones = 0;
        return solicitud;
    }

    public UUID getId() {
        return id;
    }

    public ProcesoFirma getProcesoFirma() {
        return procesoFirma;
    }

    public FirmanteProceso getFirmanteProceso() {
        return firmanteProceso;
    }

    public EstadoSolicitudFirma getEstado() {
        return estado;
    }

    public EstadoEntregaSolicitud getEstadoEntrega() {
        return estadoEntrega;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public OffsetDateTime getFechaExpiracionEnlace() {
        return fechaExpiracionEnlace;
    }

    public OffsetDateTime getFechaUltimoEnvio() {
        return fechaUltimoEnvio;
    }

    public OffsetDateTime getFechaFirma() {
        return fechaFirma;
    }

    public int getNumeroRenovaciones() {
        return numeroRenovaciones;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return fechaExpiracionEnlace == null || fechaExpiracionEnlace.isAfter(ahora);
    }

    public void marcarNotificada(OffsetDateTime ahora, OffsetDateTime expiracionEnlace) {
        this.estado = EstadoSolicitudFirma.NOTIFICADA;
        this.estadoEntrega = EstadoEntregaSolicitud.ENVIADA;
        this.fechaUltimoEnvio = ahora;
        this.fechaExpiracionEnlace = expiracionEnlace;
    }

    public void marcarErrorEntrega(OffsetDateTime ahora) {
        this.estadoEntrega = EstadoEntregaSolicitud.ERROR;
        this.fechaUltimoEnvio = ahora;
    }

    public void marcarFirmada(OffsetDateTime ahora) {
        this.estado = EstadoSolicitudFirma.FIRMADA;
        this.fechaFirma = ahora;
    }

    public void renovar(LocalDate nuevaFechaLimite, OffsetDateTime ahora, OffsetDateTime expiracionEnlace) {
        if (nuevaFechaLimite != null) {
            this.fechaLimite = nuevaFechaLimite;
        }
        this.estado = EstadoSolicitudFirma.NOTIFICADA;
        this.estadoEntrega = EstadoEntregaSolicitud.ENVIADA;
        this.fechaUltimoEnvio = ahora;
        this.fechaExpiracionEnlace = expiracionEnlace;
        this.numeroRenovaciones++;
    }

    public void revocar() {
        this.estado = EstadoSolicitudFirma.REVOCADA;
    }
}
