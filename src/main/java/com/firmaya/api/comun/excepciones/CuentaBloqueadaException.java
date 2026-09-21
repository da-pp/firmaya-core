package com.firmaya.api.comun.excepciones;

import java.time.OffsetDateTime;

public class CuentaBloqueadaException extends RuntimeException {
    private final OffsetDateTime bloqueadaHasta;

    public CuentaBloqueadaException(String mensaje, OffsetDateTime bloqueadaHasta) {
        super(mensaje);
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public OffsetDateTime getBloqueadaHasta() {
        return bloqueadaHasta;
    }
}
