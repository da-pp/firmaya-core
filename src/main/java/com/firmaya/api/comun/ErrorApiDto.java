package com.firmaya.api.comun;

import java.time.OffsetDateTime;

public record ErrorApiDto(
        String codigo,
        String mensaje,
        Object detalles,
        String idTraza,
        OffsetDateTime fechaHora
) {
    public static ErrorApiDto de(String codigo, String mensaje) {
        return new ErrorApiDto(codigo, mensaje, null, null, OffsetDateTime.now());
    }

    public static ErrorApiDto de(String codigo, String mensaje, Object detalles) {
        return new ErrorApiDto(codigo, mensaje, detalles, null, OffsetDateTime.now());
    }
}
