package com.firmaya.api.procesofirma;

import com.firmaya.api.participantes.Participante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.FirmanteProceso: instantanea congelada (nombre/correo) del firmante en el proceso. */
@Entity
@Table(name = "FirmanteProceso", schema = "firmaya")
public class FirmanteProceso {

    @Id
    @Column(name = "id_firmante_proceso")
    private UUID id;

    /**
     * Columna cruda requerida por las FK compuestas (id_contrato, id_proceso_firma) hacia
     * ProcesoFirma y (id_contrato, id_participante) hacia Participante; se deriva siempre de
     * procesoFirma.getContrato() al crear la instancia.
     */
    @Column(name = "id_contrato", nullable = false)
    private UUID idContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proceso_firma", nullable = false)
    private ProcesoFirma procesoFirma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    @Column(name = "nombre_congelado", nullable = false, length = 150)
    private String nombreCongelado;

    @Column(name = "correo_congelado", nullable = false, length = 254)
    private String correoCongelado;

    @Column(name = "fecha_congelacion", nullable = false)
    private OffsetDateTime fechaCongelacion;

    protected FirmanteProceso() {
    }

    public static FirmanteProceso crear(UUID id, ProcesoFirma procesoFirma, Participante participante,
                                         OffsetDateTime ahora) {
        FirmanteProceso firmante = new FirmanteProceso();
        firmante.id = id;
        firmante.idContrato = procesoFirma.getContrato().getId();
        firmante.procesoFirma = procesoFirma;
        firmante.participante = participante;
        firmante.nombreCongelado = participante.getNombre();
        firmante.correoCongelado = participante.getCorreoElectronico();
        firmante.fechaCongelacion = ahora;
        return firmante;
    }

    public UUID getId() {
        return id;
    }

    public ProcesoFirma getProcesoFirma() {
        return procesoFirma;
    }

    public Participante getParticipante() {
        return participante;
    }

    public String getNombreCongelado() {
        return nombreCongelado;
    }

    public String getCorreoCongelado() {
        return correoCongelado;
    }
}
