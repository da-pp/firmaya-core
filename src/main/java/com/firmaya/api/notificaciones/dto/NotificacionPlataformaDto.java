package com.firmaya.api.notificaciones.dto;

import com.firmaya.api.notificaciones.Notificacion;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificacionPlataformaDto(
        UUID id,
        String tipoEvento,
        String titulo,
        String mensaje,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaLectura
) {
    public static NotificacionPlataformaDto desde(Notificacion notificacion) {
        return new NotificacionPlataformaDto(
                notificacion.getId(),
                notificacion.getTipoEvento().getCodigo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getFechaCreacion(),
                notificacion.getFechaLectura());
    }
}
