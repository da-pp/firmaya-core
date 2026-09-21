package com.firmaya.api.plantillas;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.dto.PlantillaAdministracionDto;
import com.firmaya.api.plantillas.dto.ResumenVersionPlantillaDto;
import com.firmaya.api.plantillas.dto.SolicitudCambiarEstadoPlantilla;
import com.firmaya.api.plantillas.dto.SolicitudCrearPlantilla;
import com.firmaya.api.plantillas.dto.SolicitudCrearVersionPlantilla;
import com.firmaya.api.plantillas.dto.VersionPlantillaDto;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-16, EP-21..EP-27. Protegido por SeguridadConfig ("/api/v1/administracion/**" exige rol
 * ADMINISTRADOR); no se repite la verificacion de rol aqui.
 */
@RestController
@RequestMapping("/api/v1/administracion/plantillas")
public class ControladorAdministracionPlantillas {

    private final ServicioAdministracionPlantillas servicioAdministracionPlantillas;

    public ControladorAdministracionPlantillas(ServicioAdministracionPlantillas servicioAdministracionPlantillas) {
        this.servicioAdministracionPlantillas = servicioAdministracionPlantillas;
    }

    @GetMapping
    public PaginaDto<PlantillaAdministracionDto> buscarPlantillas(@RequestParam(required = false) TipoContrato tipo,
                                                                    @RequestParam(required = false) EstadoPlantilla estado,
                                                                    @RequestParam(required = false) Integer pagina) {
        return servicioAdministracionPlantillas.buscarPlantillas(tipo, estado, pagina);
    }

    @PostMapping
    public ResponseEntity<PlantillaAdministracionDto> crearPlantilla(
            @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
            @Valid @RequestBody SolicitudCrearPlantilla solicitud) {
        PlantillaAdministracionDto creada = servicioAdministracionPlantillas.crearPlantilla(usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{idPlantilla}")
    public PlantillaAdministracionDto obtenerPlantilla(@PathVariable UUID idPlantilla) {
        return servicioAdministracionPlantillas.obtenerPlantilla(idPlantilla);
    }

    @GetMapping("/{idPlantilla}/versiones")
    public PaginaDto<ResumenVersionPlantillaDto> listarVersiones(@PathVariable UUID idPlantilla,
                                                                    @RequestParam(required = false) Integer pagina) {
        return servicioAdministracionPlantillas.listarVersiones(idPlantilla, pagina);
    }

    @GetMapping("/{idPlantilla}/versiones/{idVersion}")
    public VersionPlantillaDto obtenerVersion(@PathVariable UUID idPlantilla, @PathVariable UUID idVersion) {
        return servicioAdministracionPlantillas.obtenerVersion(idPlantilla, idVersion);
    }

    @PostMapping("/{idPlantilla}/versiones")
    public ResponseEntity<VersionPlantillaDto> crearVersion(@PathVariable UUID idPlantilla,
                                                               @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                               @Valid @RequestBody SolicitudCrearVersionPlantilla solicitud) {
        VersionPlantillaDto creada = servicioAdministracionPlantillas.crearVersion(idPlantilla, usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PatchMapping("/{idPlantilla}/estado")
    public PlantillaAdministracionDto cambiarEstado(@PathVariable UUID idPlantilla,
                                                       @Valid @RequestBody SolicitudCambiarEstadoPlantilla solicitud) {
        return servicioAdministracionPlantillas.cambiarEstado(idPlantilla, solicitud);
    }
}
