package com.firmaya.api.firma;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Genera codigos OTP de 6 digitos y su verificador HMAC-SHA256 (derivacion protegida con
 * secreto del servidor, nunca un hash simple del numero); el OTP en claro solo vive en
 * memoria durante el envio del correo, nunca se persiste (CU-08).
 */
@Component
public class ServiceVerificadorOtp {

    private static final String ALGORITMO = "HmacSHA256";
    private final SecureRandom generadorAleatorio = new SecureRandom();
    private final byte[] pepper;

    public ServiceVerificadorOtp(@Value("${firmaya.firma.otp-pepper}") String pepper) {
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public String generarOtp() {
        int valor = 100000 + generadorAleatorio.nextInt(900000);
        return String.valueOf(valor);
    }

    public byte[] calcularVerificador(String otp, java.util.UUID idSolicitudFirma) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(pepper, ALGORITMO));
            String mensaje = idSolicitudFirma + ":" + otp;
            return mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("No se pudo calcular el verificador de OTP.", ex);
        }
    }

    public boolean coincide(String otp, java.util.UUID idSolicitudFirma, byte[] verificadorAlmacenado) {
        byte[] calculado = calcularVerificador(otp, idSolicitudFirma);
        return MessageDigest.isEqual(calculado, verificadorAlmacenado);
    }
}
