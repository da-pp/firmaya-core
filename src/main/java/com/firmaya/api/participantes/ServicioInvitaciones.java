package com.firmaya.api.participantes;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.ServicioAutorizacionContrato;
import com.firmaya.api.notificaciones.ServicioEnvioNotificaciones;
import com.firmaya.api.participantes.dto.InvitacionDto;
import com.firmaya.api.participantes.dto.SolicitudCrearInvitacion;
import com.firmaya.api.seguridad.ServicioHashCredencial;
import com.firmaya.api.tokens.RepositorioTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-03, EP-38 y EP-39. */
@Service
@Transactional
public class ServicioInvitaciones {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioParticipante repositorioParticipante;
    private final RepositorioInvitacion repositorioInvitacion;
    private final RepositorioTokenAcceso repositorioTokenAcceso;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;
    private final ServicioHashCredencial servicioHashCredencial;
    private final ServicioEnvioNotificaciones servicioEnvioNotificaciones;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final int duracionDias;
    private final String urlAcceso;

    public ServicioInvitaciones(RepositorioContrato repositorioContrato,
                                 RepositorioParticipante repositorioParticipante,
                                 RepositorioInvitacion repositorioInvitacion,
                                 RepositorioTokenAcceso repositorioTokenAcceso,
                                 RepositorioUsuario repositorioUsuario,
                                 ServicioAutorizacionContrato servicioAutorizacionContrato,
                                 ServicioHashCredencial servicioHashCredencial,
                                 ServicioEnvioNotificaciones servicioEnvioNotificaciones,
                                 ServicioRegistroAuditoria servicioRegistroAuditoria,
                                 @Value("${firmaya.invitacion.duracion-dias}") int duracionDias,
                                 @Value("${firmaya.frontend.url-acceso-externo}") String urlAcceso) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioParticipante = repositorioParticipante;
        this.repositorioInvitacion = repositorioInvitacion;
        this.repositorioTokenAcceso = repositorioTokenAcceso;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
        this.servicioHashCredencial = servicioHashCredencial;
        this.servicioEnvioNotificaciones = servicioEnvioNotificaciones;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.duracionDias = duracionDias;
        this.urlAcceso = urlAcceso;
    }

    public InvitacionDto crearYEnviar(UUID idContrato, UUID idUsuarioResponsable, SolicitudCrearInvitacion solicitud) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);
        if (!contrato.esEditable()) {
            throw new ConflictoEstadoException("No se pueden invitar partes en el estado actual del contrato.");
        }

        String correoNormalizado = solicitud.correoElectronico().trim().toLowerCase(Locale.ROOT);
        repositorioParticipante.findByContratoIdAndCorreoNormalizado(idContrato, correoNormalizado)
                .ifPresent(existente -> {
                    throw new SolicitudInvalidaException("Esta direccion ya ha sido invitada a este contrato.");
                });

        Usuario emisor = repositorioUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        OffsetDateTime ahora = OffsetDateTime.now();
        Participante participante = Participante.crear(UUID.randomUUID(), contrato, solicitud.nombre(),
                solicitud.correoElectronico(), solicitud.rol(), ahora);
        repositorioParticipante.save(participante);

        Invitacion invitacion = Invitacion.crear(UUID.randomUUID(), contrato, participante, emisor,
                solicitud.mensaje(), ahora);
        // Se reasigna el resultado de save(): con @Id manual, save() usa merge() y la
        // instancia original queda desconectada; enviarInvitacion debe mutar la copia
        // gestionada para que marcarEnviada/marcarErrorEnvio se sincronicen con la base.
        invitacion = repositorioInvitacion.save(invitacion);

        enviarInvitacion(contrato, participante, invitacion, emisor, ahora);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(emisor, "INVITACION_CREADA", "Invitacion", "Invitacion creada para " + participante.getRolParticipacion() + ".")
                .conEntidad(invitacion.getId())
                .conContrato(idContrato));

        return aDto(invitacion);
    }

    public InvitacionDto reintentarEntrega(UUID idContrato, UUID idInvitacion, UUID idUsuarioResponsable) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);

        Invitacion invitacion = repositorioInvitacion.findByIdAndContratoId(idInvitacion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La invitacion no existe."));
        if (invitacion.getEstado() != EstadoInvitacion.ERROR_ENVIO) {
            throw new ConflictoEstadoException("Solo se puede reintentar una invitacion cuyo envio fallo.");
        }

        Usuario emisor = repositorioUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        OffsetDateTime ahora = OffsetDateTime.now();
        enviarInvitacion(contrato, invitacion.getParticipante(), invitacion, emisor, ahora);

        return aDto(invitacion);
    }

    private void enviarInvitacion(Contrato contrato, Participante participante, Invitacion invitacion,
                                   Usuario emisor, OffsetDateTime ahora) {
        String credencialEnClaro = servicioHashCredencial.generarCredencialAleatoria();
        byte[] hash = servicioHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracionToken = ahora.plusDays(duracionDias);
        TokenAcceso token = TokenAcceso.deInvitacion(UUID.randomUUID(), invitacion.getId(), emisor, hash,
                ahora, expiracionToken);
        repositorioTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Fuiste invitado/a a participar del contrato \"" + contrato.getNombre()
                + "\" en FirmaYA como " + participante.getRolParticipacion() + ". "
                + (invitacion.getMensajePersonalizado() != null ? invitacion.getMensajePersonalizado() + " " : "")
                + "Accede con el siguiente enlace (valido " + duracionDias + " dias): " + enlace;
        String resumenParaRegistro = "Se envio una invitacion de acceso al contrato, valida " + duracionDias + " dias.";

        boolean enviado = servicioEnvioNotificaciones.enviarCorreoDirigidoAParticipante(contrato.getId(),
                participante.getId(), participante.getCorreoElectronico(), "CONTRATO_INVITACION_RECIBIDA",
                "Invitacion a contrato - FirmaYA", cuerpoCorreo, resumenParaRegistro);
        if (enviado) {
            invitacion.marcarEnviada(ahora, expiracionToken);
        } else {
            invitacion.marcarErrorEnvio(ahora);
        }
    }

    private InvitacionDto aDto(Invitacion invitacion) {
        return new InvitacionDto(invitacion.getId(), invitacion.getParticipante().getId(), invitacion.getEstado(),
                invitacion.getFechaExpiracion(), invitacion.getFechaCreacion(), invitacion.getFechaUltimoEnvio());
    }
}
