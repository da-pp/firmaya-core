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
public class ControladorParticipantes {

    private final ServicioConsultaParticipantes servicioConsultaParticipantes;
    private final ServicioInvitaciones servicioInvitaciones;
    private final ServicioAccesoConsulta servicioAccesoConsulta;

    public ControladorParticipantes(ServicioConsultaParticipantes servicioConsultaParticipantes,
                                     ServicioInvitaciones servicioInvitaciones,
                                     ServicioAccesoConsulta servicioAccesoConsulta) {
        this.servicioConsultaParticipantes = servicioConsultaParticipantes;
        this.servicioInvitaciones = servicioInvitaciones;
        this.servicioAccesoConsulta = servicioAccesoConsulta;
    }

    @GetMapping("/participantes")
    public List<ParticipanteDto> listarParticipantes(@PathVariable UUID idContrato) {
        return servicioConsultaParticipantes.listarParticipantes(idContrato);
    }

    @PostMapping("/invitaciones")
    public ResponseEntity<InvitacionDto> crearInvitacion(@PathVariable UUID idContrato,
                                                           @AuthenticationPrincipal ContextoUsuarioAutenticado usuario,
                                                           @Valid @RequestBody SolicitudCrearInvitacion solicitud) {
        InvitacionDto invitacion = servicioInvitaciones.crearYEnviar(idContrato, usuario.idUsuario(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitacion);
    }

    @PostMapping("/invitaciones/{idInvitacion}/reintentar")
    public InvitacionDto reintentarInvitacion(@PathVariable UUID idContrato, @PathVariable UUID idInvitacion,
                                               @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        return servicioInvitaciones.reintentarEntrega(idContrato, idInvitacion, usuario.idUsuario());
    }

    @PostMapping("/participantes/{idParticipante}/enlaces-consulta")
    public ResponseEntity<EmisionEnlaceConsultaDto> emitirEnlaceConsulta(@PathVariable UUID idContrato,
                                                                           @PathVariable UUID idParticipante,
                                                                           @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        EmisionEnlaceConsultaDto emision = servicioAccesoConsulta.emitirEnlace(idContrato, idParticipante, usuario.idUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(emision);
    }
}
