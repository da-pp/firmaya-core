package com.firmaya.api.notificaciones;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CanalPreferidoUsuarioId implements Serializable {

    private UUID idUsuario;
    private CanalNotificacion canal;

    protected CanalPreferidoUsuarioId() {
    }

    public CanalPreferidoUsuarioId(UUID idUsuario, CanalNotificacion canal) {
        this.idUsuario = idUsuario;
        this.canal = canal;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof CanalPreferidoUsuarioId that)) {
            return false;
        }
        return Objects.equals(idUsuario, that.idUsuario) && canal == that.canal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, canal);
    }
}
