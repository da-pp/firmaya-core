package com.firmaya.api.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceHashCredencialTest {

    private final ServiceHashCredencial service = new ServiceHashCredencial();

    @Test
    void generaCredencialesAleatoriasUnicasYDeLongitudAdecuada() {
        String primera = service.generarCredencialAleatoria();
        String segunda = service.generarCredencialAleatoria();

        assertThat(primera).isNotEqualTo(segunda);
        assertThat(primera).matches("^[A-Za-z0-9_-]+$");
        assertThat(primera.length()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void elHashEsDeterministaParaElMismoValor() {
        String credencial = "credencial-de-prueba";

        byte[] hash1 = service.hashear(credencial);
        byte[] hash2 = service.hashear(credencial);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(32);
    }

    @Test
    void credencialesDistintasProducenHashesDistintos() {
        byte[] hashA = service.hashear("valor-a");
        byte[] hashB = service.hashear("valor-b");

        assertThat(hashA).isNotEqualTo(hashB);
    }
}
