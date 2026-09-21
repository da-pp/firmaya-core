package com.firmaya.api.firma;

import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.ProcesoFirma;
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

/** Mapea firmaya.Firma. Fila inmutable (protegida por trigger en base de datos); evidencia de firma unica por firmante. */
@Entity
@Table(name = "Firma", schema = "firmaya")
public class Firma {

    @Id
    @Column(name = "id_firma")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_firma", nullable = false)
    private SolicitudFirma solicitudFirma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proceso_firma", nullable = false)
    private ProcesoFirma procesoFirma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_firmante_proceso", nullable = false)
    private FirmanteProceso firmanteProceso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aceptacion", nullable = false)
    private AceptacionFirma aceptacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_desafio_otp", nullable = false)
    private DesafioOtp desafioOtp;

    @Column(name = "id_version_contrato", nullable = false)
    private UUID idVersionContrato;

    @Column(name = "hash_version", nullable = false, length = 64)
    private String hashVersion;

    @Column(name = "fecha_firma", nullable = false)
    private OffsetDateTime fechaFirma;

    @Column(name = "direccion_ip", nullable = false, length = 45)
    private String direccionIp;

    @Column(name = "verificacion_otp_exitosa", nullable = false)
    private boolean verificacionOtpExitosa;

    protected Firma() {
    }

    public static Firma crear(UUID id, SolicitudFirma solicitudFirma, ProcesoFirma procesoFirma,
                               FirmanteProceso firmanteProceso, AceptacionFirma aceptacion, DesafioOtp desafioOtp,
                               UUID idVersionContrato, String hashVersion, String direccionIp,
                               OffsetDateTime ahora) {
        Firma firma = new Firma();
        firma.id = id;
        firma.solicitudFirma = solicitudFirma;
        firma.procesoFirma = procesoFirma;
        firma.firmanteProceso = firmanteProceso;
        firma.aceptacion = aceptacion;
        firma.desafioOtp = desafioOtp;
        firma.idVersionContrato = idVersionContrato;
        firma.hashVersion = hashVersion;
        firma.direccionIp = direccionIp;
        firma.verificacionOtpExitosa = true;
        firma.fechaFirma = ahora;
        return firma;
    }

    public UUID getId() {
        return id;
    }

    public SolicitudFirma getSolicitudFirma() {
        return solicitudFirma;
    }

    public FirmanteProceso getFirmanteProceso() {
        return firmanteProceso;
    }

    public UUID getIdVersionContrato() {
        return idVersionContrato;
    }

    public String getHashVersion() {
        return hashVersion;
    }

    public OffsetDateTime getFechaFirma() {
        return fechaFirma;
    }

    public String getDireccionIp() {
        return direccionIp;
    }
}
