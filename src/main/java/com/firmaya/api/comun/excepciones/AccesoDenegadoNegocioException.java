package com.firmaya.api.comun.excepciones;

public class AccesoDenegadoNegocioException extends RuntimeException {
    public AccesoDenegadoNegocioException(String mensaje) {
        super(mensaje);
    }
}
