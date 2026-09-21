package com.firmaya.api.activacion;

import com.firmaya.api.activacion.dto.ResultadoActivacionDto;
import com.firmaya.api.activacion.dto.SolicitudCompletarActivacion;
import com.firmaya.api.activacion.dto.SolicitudTokenActivacion;
import com.firmaya.api.activacion.dto.ValidacionActivacionDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-15, EP-04 y EP-05. Publico: solo autorizado por posesion del token de un solo uso. */
@RestController
@RequestMapping("/api/v1/autenticacion/activaciones")
public class ControladorActivacion {

    private final ServicioActivacion servicioActivacion;

    public ControladorActivacion(ServicioActivacion servicioActivacion) {
        this.servicioActivacion = servicioActivacion;
    }

    @PostMapping("/validar")
    public ValidacionActivacionDto validarToken(@Valid @RequestBody SolicitudTokenActivacion solicitud) {
        return servicioActivacion.validarToken(solicitud.token());
    }

    @PostMapping("/completar")
    public ResultadoActivacionDto completarActivacion(@Valid @RequestBody SolicitudCompletarActivacion solicitud) {
        return servicioActivacion.completarActivacion(solicitud);
    }
}
