package com.firmaya.api.notificaciones;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PreferenciaNotificacionId implements Serializable {

    private UUID idUsuario;
    private UUID idTipoEvento;

    protected PreferenciaNotificacionId() {
    }

    public PreferenciaNotificacionId(UUID idUsuario, UUID idTipoEvento) {
        this.idUsuario = idUsuario;
        this.idTipoEvento = idTipoEvento;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof PreferenciaNotificacionId that)) {
            return false;
        }
        return Objects.equals(idUsuario, that.idUsuario) && Objects.equals(idTipoEvento, that.idTipoEvento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idTipoEvento);
    }
}
