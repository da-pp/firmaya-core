package com.firmaya.api.recuperacion.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CU-21: la contrasena nueva debe tener minimo 8 caracteres, 1 mayuscula, 1 numero y 1 caracter especial. */
class SolicitudCompletarRecuperacionValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Testing123!", "Otra$Clave9", "Abcdefg1#", "SINMINUSCULA1!"})
    void aceptaContrasenasQueCumplenTodosLosRequisitos(String contrasena) {
        var solicitud = new SolicitudCompletarRecuperacion("token-valido", contrasena, contrasena);

        Set<ConstraintViolation<SolicitudCompletarRecuperacion>> violaciones = validator.validate(solicitud);

        assertThat(violaciones).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sinmayuscula1!",
            "SinNumero!",
            "SinCaracterEspecial1",
            "Corta1!",
            ""
    })
    void rechazaContrasenasQueIncumplenAlgunRequisito(String contrasena) {
        var solicitud = new SolicitudCompletarRecuperacion("token-valido", contrasena, contrasena);

        Set<ConstraintViolation<SolicitudCompletarRecuperacion>> violaciones = validator.validate(solicitud);

        assertThat(violaciones).isNotEmpty();
    }
}
