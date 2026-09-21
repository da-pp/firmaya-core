package com.firmaya.api.comentarios;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comentarios.dto.ComentarioDto;
import com.firmaya.api.comentarios.dto.SolicitudCrearComentario;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-06, EP-41 y EP-42. Accesible por sesion interna (Responsable/Firmante/Revisor) o externa. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}/versiones/{idVersion}/comentarios")
public class ControladorComentarios {

    private final ServicioComentarios servicioComentarios;

    public ControladorComentarios(ServicioComentarios servicioComentarios) {
        this.servicioComentarios = servicioComentarios;
    }

    @GetMapping
    public List<ComentarioDto> listar(@PathVariable UUID idContrato, @PathVariable UUID idVersion,
                                       Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioComentarios.listarPorVersionParaUsuarioInterno(idContrato, idVersion, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioComentarios.listarPorVersionParaParticipanteExterno(idContrato, idVersion, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping
    public ResponseEntity<ComentarioDto> publicar(@PathVariable UUID idContrato, @PathVariable UUID idVersion,
                                                    Authentication autenticacion,
                                                    @Valid @RequestBody SolicitudCrearComentario solicitud) {
        Object principal = autenticacion.getPrincipal();
        ComentarioDto comentario;
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            comentario = servicioComentarios.publicarComoUsuarioInterno(idContrato, idVersion, interno.idUsuario(), solicitud);
        } else if (principal instanceof ContextoParticipanteAutenticado externo) {
            comentario = servicioComentarios.publicarComoParticipanteExterno(idContrato, idVersion, externo, solicitud);
        } else {
            throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }
}
