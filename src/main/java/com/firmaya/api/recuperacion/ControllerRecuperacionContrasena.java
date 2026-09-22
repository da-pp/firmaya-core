package com.firmaya.api.recuperacion;

import com.firmaya.api.comun.MensajeDto;
import com.firmaya.api.recuperacion.dto.SolicitudCompletarRecuperacion;
import com.firmaya.api.recuperacion.dto.SolicitudRecuperacionContrasena;
import com.firmaya.api.recuperacion.dto.SolicitudTokenRecuperacion;
import com.firmaya.api.recuperacion.dto.ValidacionRecuperacionDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-21, EP-06..EP-08. Publico: solo autorizado por posesion del token de un solo uso. */
@RestController
@RequestMapping("/api/v1/autenticacion/recuperacion-contrasena")
public class ControllerRecuperacionContrasena {

    private final ServiceRecuperacionContrasena serviceRecuperacionContrasena;

    public ControllerRecuperacionContrasena(ServiceRecuperacionContrasena serviceRecuperacionContrasena) {
        this.serviceRecuperacionContrasena = serviceRecuperacionContrasena;
    }

    @PostMapping
    public MensajeDto solicitarRecuperacion(@Valid @RequestBody SolicitudRecuperacionContrasena solicitud) {
        return serviceRecuperacionContrasena.solicitarRecuperacion(solicitud.correoElectronico());
    }

    @PostMapping("/validar")
    public ValidacionRecuperacionDto validarToken(@Valid @RequestBody SolicitudTokenRecuperacion solicitud) {
        return serviceRecuperacionContrasena.validarToken(solicitud.token());
    }

    @PostMapping("/completar")
    public MensajeDto completarRecuperacion(@Valid @RequestBody SolicitudCompletarRecuperacion solicitud) {
        return serviceRecuperacionContrasena.completarRecuperacion(solicitud);
    }
}
