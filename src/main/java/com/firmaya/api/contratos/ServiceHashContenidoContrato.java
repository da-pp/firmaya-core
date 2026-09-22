package com.firmaya.api.contratos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Calcula el SHA-256 del contenido de una version de contrato (CU-01, CU-02, CU-13, CU-14).
 * Canonizacion: [PENDIENTE] en la especificacion (PD-07). Se usa la representacion mas simple
 * y verificable posible -- bytes UTF-8 exactos del campo `contenido` tal como se persiste, sin
 * normalizacion adicional -- documentada como decision tecnica provisional.
 */
@Component
public class ServiceHashContenidoContrato {

    public String calcular(String contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resultado = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", ex);
        }
    }
}
