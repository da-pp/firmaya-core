package com.firmaya.api.notificaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.PreferenciaNotificacion (clave compuesta id_usuario + id_tipo_evento). */
@Entity
@Table(name = "PreferenciaNotificacion", schema = "firmaya")
@IdClass(PreferenciaNotificacionId.class)
public class PreferenciaNotificacion {

    @Id
    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Id
    @Column(name = "id_tipo_evento")
    private UUID idTipoEvento;

    @Column(name = "habilitada", nullable = false)
    private boolean habilitada;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(UUID idUsuario, UUID idTipoEvento, boolean habilitada, OffsetDateTime ahora) {
        this.idUsuario = idUsuario;
        this.idTipoEvento = idTipoEvento;
        this.habilitada = habilitada;
        this.fechaActualizacion = ahora;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdTipoEvento() {
        return idTipoEvento;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void actualizar(boolean habilitada, OffsetDateTime ahora) {
        this.habilitada = habilitada;
        this.fechaActualizacion = ahora;
    }
}
