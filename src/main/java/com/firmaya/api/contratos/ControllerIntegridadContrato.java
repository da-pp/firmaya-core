package com.firmaya.api.contratos;

import com.firmaya.api.contratos.dto.SolicitudVerificarIntegridad;
import com.firmaya.api.contratos.dto.VerificacionIntegridadDto;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-13, EP-36. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}/verificaciones-integridad")
public class ControllerIntegridadContrato {

    private final ServiceIntegridadContrato serviceIntegridadContrato;

    public ControllerIntegridadContrato(ServiceIntegridadContrato serviceIntegridadContrato) {
        this.serviceIntegridadContrato = serviceIntegridadContrato;
    }

    @PostMapping
    public VerificacionIntegridadDto verificar(@PathVariable UUID idContrato,
                                                @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                @Valid @RequestBody SolicitudVerificarIntegridad solicitud) {
        return serviceIntegridadContrato.verificar(idContrato, usuario.idUsuario(), solicitud);
    }
}
