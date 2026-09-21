package com.firmaya.api.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServicioHashCredencialTest {

    private final ServicioHashCredencial servicio = new ServicioHashCredencial();

    @Test
    void generaCredencialesAleatoriasUnicasYDeLongitudAdecuada() {
        String primera = servicio.generarCredencialAleatoria();
        String segunda = servicio.generarCredencialAleatoria();

        assertThat(primera).isNotEqualTo(segunda);
        assertThat(primera).matches("^[A-Za-z0-9_-]+$");
        assertThat(primera.length()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void elHashEsDeterministaParaElMismoValor() {
        String credencial = "credencial-de-prueba";

        byte[] hash1 = servicio.hashear(credencial);
        byte[] hash2 = servicio.hashear(credencial);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(32);
    }

    @Test
    void credencialesDistintasProducenHashesDistintos() {
        byte[] hashA = servicio.hashear("valor-a");
        byte[] hashB = servicio.hashear("valor-b");

        assertThat(hashA).isNotEqualTo(hashB);
    }
}
