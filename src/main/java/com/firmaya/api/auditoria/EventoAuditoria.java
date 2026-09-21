package com.firmaya.api.auditoria;

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
 * Mapea firmaya.EventoAuditoria. Fila inmutable (protegida por trigger en base de datos):
 * nunca se actualiza ni se borra desde la aplicacion. NUNCA persistir contrasenas, hashes de
 * contrasena, tokens u OTP en descripcion/datos_json.
 */
@Entity
@Table(name = "EventoAuditoria", schema = "firmaya")
public class EventoAuditoria {

    @Id
    @Column(name = "id_evento_auditoria")
    private UUID id;

    @Column(name = "fecha_evento", nullable = false)
    private OffsetDateTime fechaEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actor", nullable = false, length = 12)
    private TipoActorAuditoria tipoActor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actor")
    private Usuario usuarioActor;

    @Column(name = "tipo_accion", nullable = false, length = 80)
    private String tipoAccion;

    @Column(name = "tipo_entidad", nullable = false, length = 60)
    private String tipoEntidad;

    @Column(name = "id_entidad")
    private UUID idEntidad;

    @Column(name = "id_contrato")
    private UUID idContrato;

    @Column(name = "id_version_contrato")
    private UUID idVersionContrato;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "datos_anteriores_json")
    private String datosAnterioresJson;

    @Column(name = "datos_posteriores_json")
    private String datosPosterioresJson;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "identificador_traza", length = 100)
    private String identificadorTraza;

    protected EventoAuditoria() {
    }

    EventoAuditoria(UUID id, OffsetDateTime fechaEvento, TipoActorAuditoria tipoActor, Usuario usuarioActor,
                    String tipoAccion, String tipoEntidad, UUID idEntidad, UUID idContrato, UUID idVersionContrato,
                    String descripcion, String datosAnterioresJson, String datosPosterioresJson, String hashSha256,
                    String direccionIp, String identificadorTraza) {
        this.id = id;
        this.fechaEvento = fechaEvento;
        this.tipoActor = tipoActor;
        this.usuarioActor = usuarioActor;
        this.tipoAccion = tipoAccion;
        this.tipoEntidad = tipoEntidad;
        this.idEntidad = idEntidad;
        this.idContrato = idContrato;
        this.idVersionContrato = idVersionContrato;
        this.descripcion = descripcion;
        this.datosAnterioresJson = datosAnterioresJson;
        this.datosPosterioresJson = datosPosterioresJson;
        this.hashSha256 = hashSha256;
        this.direccionIp = direccionIp;
        this.identificadorTraza = identificadorTraza;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getFechaEvento() {
        return fechaEvento;
    }

    public TipoActorAuditoria getTipoActor() {
        return tipoActor;
    }

    public Usuario getUsuarioActor() {
        return usuarioActor;
    }

    public String getTipoAccion() {
        return tipoAccion;
    }

    public String getTipoEntidad() {
        return tipoEntidad;
    }

    public UUID getIdEntidad() {
        return idEntidad;
    }

    public UUID getIdContrato() {
        return idContrato;
    }

    public UUID getIdVersionContrato() {
        return idVersionContrato;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDatosAnterioresJson() {
        return datosAnterioresJson;
    }

    public String getDatosPosterioresJson() {
        return datosPosterioresJson;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public String getIdentificadorTraza() {
        return identificadorTraza;
    }
}
