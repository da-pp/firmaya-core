package com.firmaya.api.usuarios;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.usuarios.dto.ReenvioActivacionDto;
import com.firmaya.api.usuarios.dto.SolicitudActualizarUsuario;
import com.firmaya.api.usuarios.dto.SolicitudCambiarEstadoUsuario;
import com.firmaya.api.usuarios.dto.SolicitudCrearUsuario;
import com.firmaya.api.usuarios.dto.UsuarioAdministracionDto;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-15, EP-13..EP-18. Protegido por SeguridadConfig ("/api/v1/administracion/**" exige rol
 * ADMINISTRADOR); no se repite la verificacion de rol aqui.
 */
@RestController
@RequestMapping("/api/v1/administracion/usuarios")
public class ControllerAdministracionUsuarios {

    private final ServiceAdministracionUsuarios serviceAdministracionUsuarios;

    public ControllerAdministracionUsuarios(ServiceAdministracionUsuarios serviceAdministracionUsuarios) {
        this.serviceAdministracionUsuarios = serviceAdministracionUsuarios;
    }

    @GetMapping
    public PaginaDto<UsuarioAdministracionDto> buscarUsuarios(@RequestParam(required = false) String correoElectronico,
                                                                @RequestParam(required = false) RolGlobal rol,
                                                                @RequestParam(required = false) EstadoAdministrativo estado,
                                                                @RequestParam(required = false) Integer pagina) {
        return serviceAdministracionUsuarios.buscarUsuarios(correoElectronico, rol, estado, pagina);
    }

    @PostMapping
    public ResponseEntity<UsuarioAdministracionDto> crearUsuario(@Valid @RequestBody SolicitudCrearUsuario solicitud) {
        UsuarioAdministracionDto creado = serviceAdministracionUsuarios.crearUsuario(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{idUsuario}")
    public UsuarioAdministracionDto obtenerUsuario(@PathVariable UUID idUsuario) {
        return serviceAdministracionUsuarios.obtenerUsuario(idUsuario);
    }

    @PatchMapping("/{idUsuario}")
    public UsuarioAdministracionDto actualizarUsuario(@PathVariable UUID idUsuario,
                                                        @Valid @RequestBody SolicitudActualizarUsuario solicitud) {
        return serviceAdministracionUsuarios.actualizarUsuario(idUsuario, solicitud);
    }

    @PatchMapping("/{idUsuario}/estado")
    public UsuarioAdministracionDto cambiarEstado(@PathVariable UUID idUsuario,
                                                    @Valid @RequestBody SolicitudCambiarEstadoUsuario solicitud) {
        return serviceAdministracionUsuarios.cambiarEstadoAdministrativo(idUsuario, solicitud);
    }

    @PostMapping("/{idUsuario}/reenviar-activacion")
    public ReenvioActivacionDto reenviarActivacion(@PathVariable UUID idUsuario) {
        return serviceAdministracionUsuarios.reenviarActivacion(idUsuario);
    }
}
