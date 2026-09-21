package com.firmaya.api.comun.excepciones;

public class ConflictoEstadoException extends RuntimeException {
    public ConflictoEstadoException(String mensaje) {
        super(mensaje);
    }
}
