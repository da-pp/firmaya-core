package com.firmaya.api.contratos;

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

/** Mapea firmaya.HistorialEstadoContrato (CU-05). Solo transiciones exactas permitidas por CK_HistorialEstado_Transicion. */
@Entity
@Table(name = "HistorialEstadoContrato", schema = "firmaya")
public class HistorialEstadoContrato {

    @Id
    @Column(name = "id_historial_estado")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 19)
    private EstadoContrato estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 19)
    private EstadoContrato estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actor")
    private Usuario usuarioActor;

    @Column(name = "id_proceso_firma")
    private UUID idProcesoFirma;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "fecha_cambio", nullable = false)
    private OffsetDateTime fechaCambio;

    protected HistorialEstadoContrato() {
    }

    public static HistorialEstadoContrato crear(UUID id, Contrato contrato, EstadoContrato estadoAnterior,
                                                  EstadoContrato estadoNuevo, Usuario usuarioActor,
                                                  UUID idProcesoFirma, String motivo, OffsetDateTime ahora) {
        HistorialEstadoContrato historial = new HistorialEstadoContrato();
        historial.id = id;
        historial.contrato = contrato;
        historial.estadoAnterior = estadoAnterior;
        historial.estadoNuevo = estadoNuevo;
        historial.usuarioActor = usuarioActor;
        historial.idProcesoFirma = idProcesoFirma;
        historial.motivo = motivo;
        historial.fechaCambio = ahora;
        return historial;
    }
}
