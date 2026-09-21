package com.firmaya.api.firma;

import com.firmaya.api.procesofirma.SolicitudFirma;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.AceptacionFirma (CU-08, paso previo obligatorio al envio del OTP). */
@Entity
@Table(name = "AceptacionFirma", schema = "firmaya")
public class AceptacionFirma {

    @Id
    @Column(name = "id_aceptacion")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_firma", nullable = false)
    private SolicitudFirma solicitudFirma;

    @Column(name = "id_version_contrato", nullable = false)
    private UUID idVersionContrato;

    @Column(name = "hash_version", nullable = false, length = 64)
    private String hashVersion;

    @Column(name = "texto_aceptacion", nullable = false, length = 1000)
    private String textoAceptacion;

    @Column(name = "fecha_aceptacion", nullable = false)
    private OffsetDateTime fechaAceptacion;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    protected AceptacionFirma() {
    }

    public static AceptacionFirma crear(UUID id, SolicitudFirma solicitudFirma, UUID idVersionContrato,
                                         String hashVersion, String textoAceptacion, String direccionIp,
                                         OffsetDateTime ahora) {
        AceptacionFirma aceptacion = new AceptacionFirma();
        aceptacion.id = id;
        aceptacion.solicitudFirma = solicitudFirma;
        aceptacion.idVersionContrato = idVersionContrato;
        aceptacion.hashVersion = hashVersion;
        aceptacion.textoAceptacion = textoAceptacion;
        aceptacion.direccionIp = direccionIp;
        aceptacion.fechaAceptacion = ahora;
        return aceptacion;
    }

    public UUID getId() {
        return id;
    }

    public SolicitudFirma getSolicitudFirma() {
        return solicitudFirma;
    }

    public UUID getIdVersionContrato() {
        return idVersionContrato;
    }

    public String getHashVersion() {
        return hashVersion;
    }

    public OffsetDateTime getFechaAceptacion() {
        return fechaAceptacion;
    }
}
