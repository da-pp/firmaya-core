package com.firmaya.api.contratos;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.contratos.dto.ComparacionVersionesDto;
import com.firmaya.api.contratos.dto.DetalleContratoDto;
import com.firmaya.api.contratos.dto.ResumenContratoDto;
import com.firmaya.api.contratos.dto.ResumenVersionContratoDto;
import com.firmaya.api.contratos.dto.SolicitudCrearContrato;
import com.firmaya.api.contratos.dto.SolicitudGuardarVersionContrato;
import com.firmaya.api.contratos.dto.SolicitudRestaurarVersionContrato;
import com.firmaya.api.contratos.dto.VersionContratoDto;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-01, CU-02, CU-04, CU-11, CU-12, CU-14: EP-28 (listar), EP-29 (crear), EP-30 (obtener),
 * EP-31 (historial), EP-32 (version puntual), EP-33 (guardar version), EP-34 (comparar),
 * EP-35 (restaurar).
 */
@RestController
@RequestMapping("/api/v1/contratos")
public class ControladorContratos {

    private final ServicioConsultaContratos servicioConsultaContratos;
    private final ServicioGestionContratos servicioGestionContratos;
    private final ServicioGestionVersionesContrato servicioGestionVersionesContrato;
    private final ServicioConsultaVersionesContrato servicioConsultaVersionesContrato;
    private final ServicioComparacionContratos servicioComparacionContratos;

    public ControladorContratos(ServicioConsultaContratos servicioConsultaContratos,
                                 ServicioGestionContratos servicioGestionContratos,
                                 ServicioGestionVersionesContrato servicioGestionVersionesContrato,
                                 ServicioConsultaVersionesContrato servicioConsultaVersionesContrato,
                                 ServicioComparacionContratos servicioComparacionContratos) {
        this.servicioConsultaContratos = servicioConsultaContratos;
        this.servicioGestionContratos = servicioGestionContratos;
        this.servicioGestionVersionesContrato = servicioGestionVersionesContrato;
        this.servicioConsultaVersionesContrato = servicioConsultaVersionesContrato;
        this.servicioComparacionContratos = servicioComparacionContratos;
    }

    @GetMapping
    public PaginaDto<ResumenContratoDto> listar(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                 @RequestParam(required = false) EstadoContrato estado,
                                                 @RequestParam(required = false) String texto,
                                                 @RequestParam(required = false) Integer pagina) {
        return servicioConsultaContratos.buscarContratosAccesibles(usuario.idUsuario(), estado, texto, pagina);
    }

    @PostMapping
    public ResponseEntity<DetalleContratoDto> crear(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                      @Valid @RequestBody SolicitudCrearContrato solicitud) {
        DetalleContratoDto creado = servicioGestionContratos.crearDesdePlantilla(usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{idContrato}")
    public DetalleContratoDto obtener(@PathVariable UUID idContrato, Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioConsultaContratos.obtenerParaUsuarioInterno(idContrato, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioConsultaContratos.obtenerParaParticipanteExterno(idContrato, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @GetMapping("/{idContrato}/versiones")
    public PaginaDto<ResumenVersionContratoDto> listarHistorial(@PathVariable UUID idContrato,
                                                                   Authentication autenticacion,
                                                                   @RequestParam(required = false) Integer pagina) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioConsultaVersionesContrato.listarHistorialParaUsuarioInterno(idContrato, interno.idUsuario(), pagina);
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioConsultaVersionesContrato.listarHistorialParaParticipanteExterno(idContrato, externo, pagina);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping("/{idContrato}/versiones")
    public ResponseEntity<VersionContratoDto> guardarVersion(@PathVariable UUID idContrato,
                                                               @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                               @Valid @RequestBody SolicitudGuardarVersionContrato solicitud) {
        VersionContratoDto version = servicioGestionVersionesContrato.guardarNuevaVersion(idContrato,
                usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @GetMapping("/{idContrato}/versiones/comparar")
    public ComparacionVersionesDto compararVersiones(@PathVariable UUID idContrato,
                                                       Authentication autenticacion,
                                                       @RequestParam UUID idVersionOrigen,
                                                       @RequestParam UUID idVersionDestino) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioComparacionContratos.compararParaUsuarioInterno(idContrato, interno.idUsuario(),
                    idVersionOrigen, idVersionDestino);
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioComparacionContratos.compararParaParticipanteExterno(idContrato, externo,
                    idVersionOrigen, idVersionDestino);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @GetMapping("/{idContrato}/versiones/{idVersion}")
    public VersionContratoDto obtenerVersion(@PathVariable UUID idContrato, @PathVariable UUID idVersion,
                                              Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioConsultaVersionesContrato.obtenerVersionParaUsuarioInterno(idContrato, idVersion, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioConsultaVersionesContrato.obtenerVersionParaParticipanteExterno(idContrato, idVersion, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping("/{idContrato}/versiones/{idVersion}/restaurar")
    public ResponseEntity<VersionContratoDto> restaurarVersion(@PathVariable UUID idContrato,
                                                                 @PathVariable UUID idVersion,
                                                                 @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                                 @Valid @RequestBody SolicitudRestaurarVersionContrato solicitud) {
        VersionContratoDto version = servicioGestionVersionesContrato.restaurarVersion(idContrato, idVersion,
                usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }
}
