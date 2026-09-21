package com.firmaya.api.procesofirma;

import com.firmaya.api.procesofirma.dto.CambioEstadoContratoDto;
import com.firmaya.api.procesofirma.dto.OpcionesTransicionDto;
import com.firmaya.api.procesofirma.dto.ResumenProcesoFirmaDto;
import com.firmaya.api.procesofirma.dto.SolicitudCambiarEstadoContrato;
import com.firmaya.api.procesofirma.dto.SolicitudCancelarProcesoFirma;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-05, EP-43, EP-44 y EP-45. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}")
public class ControladorEstadosContrato {

    private final ServicioEstadosContrato servicioEstadosContrato;
    private final ServicioProcesoFirma servicioProcesoFirma;

    public ControladorEstadosContrato(ServicioEstadosContrato servicioEstadosContrato,
                                       ServicioProcesoFirma servicioProcesoFirma) {
        this.servicioEstadosContrato = servicioEstadosContrato;
        this.servicioProcesoFirma = servicioProcesoFirma;
    }

    @GetMapping("/transiciones")
    public OpcionesTransicionDto obtenerTransiciones(@PathVariable UUID idContrato,
                                                       @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioEstadosContrato.obtenerTransicionesManuales(idContrato, usuario.idUsuario());
    }

    @PostMapping("/transiciones")
    public CambioEstadoContratoDto cambiarEstado(@PathVariable UUID idContrato,
                                                  @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                  @Valid @RequestBody SolicitudCambiarEstadoContrato solicitud) {
        return servicioEstadosContrato.cambiarEstado(idContrato, usuario.idUsuario(), solicitud);
    }

    @PostMapping("/proceso-firma/cancelar")
    public ResumenProcesoFirmaDto cancelarProcesoFirma(@PathVariable UUID idContrato,
                                                        @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                        @Valid @RequestBody(required = false) SolicitudCancelarProcesoFirma solicitud) {
        SolicitudCancelarProcesoFirma cuerpo = solicitud != null ? solicitud : new SolicitudCancelarProcesoFirma(null);
        return servicioProcesoFirma.cancelarAntesPrimeraFirma(idContrato, usuario.idUsuario(), cuerpo);
    }
}
