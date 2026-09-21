package com.firmaya.api.comun.excepciones;

import java.util.Map;

public class SolicitudInvalidaException extends RuntimeException {

    private final Map<String, String> detalles;

    public SolicitudInvalidaException(String mensaje) {
        this(mensaje, Map.of());
    }

    public SolicitudInvalidaException(String mensaje, Map<String, String> detalles) {
        super(mensaje);
        this.detalles = detalles;
    }

    public Map<String, String> getDetalles() {
        return detalles;
    }
}
