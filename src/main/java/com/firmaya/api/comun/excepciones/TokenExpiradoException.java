package com.firmaya.api.comun.excepciones;

public class TokenExpiradoException extends RuntimeException {
    public TokenExpiradoException(String mensaje) {
        super(mensaje);
    }
}
