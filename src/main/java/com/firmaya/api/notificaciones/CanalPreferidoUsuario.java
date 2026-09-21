package com.firmaya.api.notificaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Mapea firmaya.CanalPreferidoUsuario (clave compuesta id_usuario + canal). */
@Entity
@Table(name = "CanalPreferidoUsuario", schema = "firmaya")
@IdClass(CanalPreferidoUsuarioId.class)
public class CanalPreferidoUsuario {

    @Id
    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 18)
    private CanalNotificacion canal;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected CanalPreferidoUsuario() {
    }

    public CanalPreferidoUsuario(UUID idUsuario, CanalNotificacion canal, OffsetDateTime ahora) {
        this.idUsuario = idUsuario;
        this.canal = canal;
        this.fechaActualizacion = ahora;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }
}
