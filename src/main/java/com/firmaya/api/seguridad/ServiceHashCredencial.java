package com.firmaya.api.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Genera credenciales opacas de un solo uso (sesion, recuperacion, etc.) y calcula su hash
 * SHA-256 para persistir; el valor en claro solo existe en memoria y en el canal de entrega
 * (cookie o correo), nunca en base de datos ni en registros de auditoria.
 */
@Component
public class ServiceHashCredencial {

    private static final int BYTES_ALEATORIOS = 32;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public String generarCredencialAleatoria() {
        byte[] datos = new byte[BYTES_ALEATORIOS];
        generadorAleatorio.nextBytes(datos);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    public byte[] hashear(String credencialEnClaro) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(credencialEnClaro.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", ex);
        }
    }
}
