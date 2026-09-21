package com.firmaya.api.procesofirma;

import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.procesofirma.dto.DetalleProcesoFirmaDto;
import com.firmaya.api.procesofirma.dto.EnvioSolicitudesFirmaDto;
import com.firmaya.api.procesofirma.dto.ResultadoEntregaLoteDto;
import com.firmaya.api.procesofirma.dto.SolicitudEnviarSolicitudesFirma;
import com.firmaya.api.procesofirma.dto.SolicitudFirmaDto;
import com.firmaya.api.procesofirma.dto.SolicitudReintentarFirmasFallidas;
import com.firmaya.api.procesofirma.dto.SolicitudRenovarSolicitudFirma;
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

/** CU-07/CU-09, EP-46, EP-47, EP-48, EP-49 y EP-50. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}/proceso-firma")
public class ControladorProcesoFirma {

    private final ServicioConsultaProcesoFirma servicioConsultaProcesoFirma;
    private final ServicioSolicitudesFirma servicioSolicitudesFirma;

    public ControladorProcesoFirma(ServicioConsultaProcesoFirma servicioConsultaProcesoFirma,
                                    ServicioSolicitudesFirma servicioSolicitudesFirma) {
        this.servicioConsultaProcesoFirma = servicioConsultaProcesoFirma;
        this.servicioSolicitudesFirma = servicioSolicitudesFirma;
    }

    @GetMapping
    public DetalleProcesoFirmaDto obtener(@PathVariable UUID idContrato,
                                           @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioConsultaProcesoFirma.obtenerActualOUltimo(idContrato, usuario.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no tiene un proceso de firma."));
    }

    @PostMapping("/solicitudes")
    public EnvioSolicitudesFirmaDto enviarSolicitudes(@PathVariable UUID idContrato,
                                                        @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                        @Valid @RequestBody SolicitudEnviarSolicitudesFirma solicitud) {
        return servicioSolicitudesFirma.enviarSolicitudesIniciales(idContrato, usuario.idUsuario(), solicitud);
    }

    @PostMapping("/solicitudes/reintentar-fallidas")
    public ResultadoEntregaLoteDto reintentarFallidas(@PathVariable UUID idContrato,
                                                        @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                        @RequestBody(required = false) SolicitudReintentarFirmasFallidas solicitud) {
        SolicitudReintentarFirmasFallidas cuerpo = solicitud != null ? solicitud : new SolicitudReintentarFirmasFallidas(null);
        return servicioSolicitudesFirma.reintentarNotificacionesFallidas(idContrato, usuario.idUsuario(), cuerpo);
    }

    @PostMapping("/solicitudes/{idSolicitud}/reenviar")
    public SolicitudFirmaDto reenviar(@PathVariable UUID idContrato, @PathVariable UUID idSolicitud,
                                       @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioSolicitudesFirma.reenviar(idContrato, usuario.idUsuario(), idSolicitud);
    }

    @PostMapping("/solicitudes/{idSolicitud}/renovar")
    public SolicitudFirmaDto renovar(@PathVariable UUID idContrato, @PathVariable UUID idSolicitud,
                                      @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                      @Valid @RequestBody SolicitudRenovarSolicitudFirma solicitud) {
        return servicioSolicitudesFirma.renovar(idContrato, usuario.idUsuario(), idSolicitud, solicitud);
    }
}
