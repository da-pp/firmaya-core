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
public class ControllerContratos {

    private final ServiceConsultaContratos serviceConsultaContratos;
    private final ServiceGestionContratos serviceGestionContratos;
    private final ServiceGestionVersionesContrato serviceGestionVersionesContrato;
    private final ServiceConsultaVersionesContrato serviceConsultaVersionesContrato;
    private final ServiceComparacionContratos serviceComparacionContratos;

    public ControllerContratos(ServiceConsultaContratos serviceConsultaContratos,
                                 ServiceGestionContratos serviceGestionContratos,
                                 ServiceGestionVersionesContrato serviceGestionVersionesContrato,
                                 ServiceConsultaVersionesContrato serviceConsultaVersionesContrato,
                                 ServiceComparacionContratos serviceComparacionContratos) {
        this.serviceConsultaContratos = serviceConsultaContratos;
        this.serviceGestionContratos = serviceGestionContratos;
        this.serviceGestionVersionesContrato = serviceGestionVersionesContrato;
        this.serviceConsultaVersionesContrato = serviceConsultaVersionesContrato;
        this.serviceComparacionContratos = serviceComparacionContratos;
    }

    @GetMapping
    public PaginaDto<ResumenContratoDto> listar(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                 @RequestParam(required = false) EstadoContrato estado,
                                                 @RequestParam(required = false) String texto,
                                                 @RequestParam(required = false) Integer pagina) {
        return serviceConsultaContratos.buscarContratosAccesibles(usuario.idUsuario(), estado, texto, pagina);
    }

    @PostMapping
    public ResponseEntity<DetalleContratoDto> crear(@AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                      @Valid @RequestBody SolicitudCrearContrato solicitud) {
        DetalleContratoDto creado = serviceGestionContratos.crearDesdePlantilla(usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{idContrato}")
    public DetalleContratoDto obtener(@PathVariable UUID idContrato, Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return serviceConsultaContratos.obtenerParaUsuarioInterno(idContrato, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return serviceConsultaContratos.obtenerParaParticipanteExterno(idContrato, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @GetMapping("/{idContrato}/versiones")
    public PaginaDto<ResumenVersionContratoDto> listarHistorial(@PathVariable UUID idContrato,
                                                                   Authentication autenticacion,
                                                                   @RequestParam(required = false) Integer pagina) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return serviceConsultaVersionesContrato.listarHistorialParaUsuarioInterno(idContrato, interno.idUsuario(), pagina);
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return serviceConsultaVersionesContrato.listarHistorialParaParticipanteExterno(idContrato, externo, pagina);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping("/{idContrato}/versiones")
    public ResponseEntity<VersionContratoDto> guardarVersion(@PathVariable UUID idContrato,
                                                               @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                               @Valid @RequestBody SolicitudGuardarVersionContrato solicitud) {
        VersionContratoDto version = serviceGestionVersionesContrato.guardarNuevaVersion(idContrato,
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
            return serviceComparacionContratos.compararParaUsuarioInterno(idContrato, interno.idUsuario(),
                    idVersionOrigen, idVersionDestino);
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return serviceComparacionContratos.compararParaParticipanteExterno(idContrato, externo,
                    idVersionOrigen, idVersionDestino);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @GetMapping("/{idContrato}/versiones/{idVersion}")
    public VersionContratoDto obtenerVersion(@PathVariable UUID idContrato, @PathVariable UUID idVersion,
                                              Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return serviceConsultaVersionesContrato.obtenerVersionParaUsuarioInterno(idContrato, idVersion, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return serviceConsultaVersionesContrato.obtenerVersionParaParticipanteExterno(idContrato, idVersion, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping("/{idContrato}/versiones/{idVersion}/restaurar")
    public ResponseEntity<VersionContratoDto> restaurarVersion(@PathVariable UUID idContrato,
                                                                 @PathVariable UUID idVersion,
                                                                 @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                                 @Valid @RequestBody SolicitudRestaurarVersionContrato solicitud) {
        VersionContratoDto version = serviceGestionVersionesContrato.restaurarVersion(idContrato, idVersion,
                usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }
}
