package com.firmaya.api.participantes;

import com.firmaya.api.participantes.dto.EmisionEnlaceConsultaDto;
import com.firmaya.api.participantes.dto.InvitacionDto;
import com.firmaya.api.participantes.dto.ParticipanteDto;
import com.firmaya.api.participantes.dto.SolicitudCrearInvitacion;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-03, EP-37, EP-38, EP-39. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}")
public class ControllerParticipantes {

    private final ServiceConsultaParticipantes serviceConsultaParticipantes;
    private final ServiceInvitaciones serviceInvitaciones;
    private final ServiceAccesoConsulta serviceAccesoConsulta;

    public ControllerParticipantes(ServiceConsultaParticipantes serviceConsultaParticipantes,
                                     ServiceInvitaciones serviceInvitaciones,
                                     ServiceAccesoConsulta serviceAccesoConsulta) {
        this.serviceConsultaParticipantes = serviceConsultaParticipantes;
        this.serviceInvitaciones = serviceInvitaciones;
        this.serviceAccesoConsulta = serviceAccesoConsulta;
    }

    @GetMapping("/participantes")
    public List<ParticipanteDto> listarParticipantes(@PathVariable UUID idContrato) {
        return serviceConsultaParticipantes.listarParticipantes(idContrato);
    }

    @PostMapping("/invitaciones")
    public ResponseEntity<InvitacionDto> crearInvitacion(@PathVariable UUID idContrato,
                                                           @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                           @Valid @RequestBody SolicitudCrearInvitacion solicitud) {
        InvitacionDto invitacion = serviceInvitaciones.crearYEnviar(idContrato, usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitacion);
    }

    @PostMapping("/invitaciones/{idInvitacion}/reintentar")
    public InvitacionDto reintentarInvitacion(@PathVariable UUID idContrato, @PathVariable UUID idInvitacion,
                                               @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return serviceInvitaciones.reintentarEntrega(idContrato, idInvitacion, usuario.idUsuario());
    }

    @PostMapping("/participantes/{idParticipante}/enlaces-consulta")
    public ResponseEntity<EmisionEnlaceConsultaDto> emitirEnlaceConsulta(@PathVariable UUID idContrato,
                                                                           @PathVariable UUID idParticipante,
                                                                           @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        EmisionEnlaceConsultaDto emision = serviceAccesoConsulta.emitirEnlace(idContrato, idParticipante, usuario.idUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(emision);
    }
}
