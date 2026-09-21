package com.firmaya.api.procesofirma;

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

/**
 * Mapea firmaya.ProcesoFirma (CU-05). Congela version/hash objetivo y, junto con
 * FirmanteProceso, la lista de firmantes al pasar a LISTO_PARA_FIRMAR.
 */
@Entity
@Table(name = "ProcesoFirma", schema = "firmaya")
public class ProcesoFirma {

    @Id
    @Column(name = "id_proceso_firma")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Column(name = "id_version_objetivo", nullable = false)
    private UUID idVersionObjetivo;

    @Column(name = "hash_objetivo", nullable = false, length = 64)
    private String hashObjetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoProcesoFirma estado;

    @Column(name = "finalizado", nullable = false)
    private boolean finalizado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private OffsetDateTime fechaCancelacion;

    @Column(name = "fecha_finalizacion")
    private OffsetDateTime fechaFinalizacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cancelador")
    private Usuario usuarioCancelador;

    protected ProcesoFirma() {
    }

    public static ProcesoFirma crear(UUID id, Contrato contrato, UUID idVersionObjetivo, String hashObjetivo,
                                      OffsetDateTime ahora) {
        ProcesoFirma proceso = new ProcesoFirma();
        proceso.id = id;
        proceso.contrato = contrato;
        proceso.idVersionObjetivo = idVersionObjetivo;
        proceso.hashObjetivo = hashObjetivo;
        proceso.estado = EstadoProcesoFirma.PREPARADO;
        proceso.finalizado = false;
        proceso.fechaCreacion = ahora;
        return proceso;
    }

    public UUID getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public UUID getIdVersionObjetivo() {
        return idVersionObjetivo;
    }

    public String getHashObjetivo() {
        return hashObjetivo;
    }

    public EstadoProcesoFirma getEstado() {
        return estado;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void activar() {
        this.estado = EstadoProcesoFirma.ACTIVO;
    }

    public void cancelar(Usuario cancelador, String motivo, OffsetDateTime ahora) {
        this.estado = EstadoProcesoFirma.CANCELADO;
        this.finalizado = true;
        this.fechaCancelacion = ahora;
        this.motivoCancelacion = motivo;
        this.usuarioCancelador = cancelador;
    }

    public void completar(OffsetDateTime ahora) {
        this.estado = EstadoProcesoFirma.COMPLETADO;
        this.finalizado = true;
        this.fechaFinalizacion = ahora;
    }
}
