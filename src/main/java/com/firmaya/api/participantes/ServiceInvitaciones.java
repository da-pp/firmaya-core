package com.firmaya.api.participantes;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.participantes.dto.InvitacionDto;
import com.firmaya.api.participantes.dto.SolicitudCrearInvitacion;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.RepositoryUsuario;
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
public class ServiceInvitaciones {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryParticipante repositoryParticipante;
    private final RepositoryInvitacion repositoryInvitacion;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final RepositoryUsuario repositoryUsuario;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final int duracionDias;
    private final String urlAcceso;

    public ServiceInvitaciones(RepositoryContrato repositoryContrato,
                                 RepositoryParticipante repositoryParticipante,
                                 RepositoryInvitacion repositoryInvitacion,
                                 RepositoryTokenAcceso repositoryTokenAcceso,
                                 RepositoryUsuario repositoryUsuario,
                                 ServiceAutorizacionContrato serviceAutorizacionContrato,
                                 ServiceHashCredencial serviceHashCredencial,
                                 ServiceEnvioNotificaciones serviceEnvioNotificaciones,
                                 ServiceRegistroAuditoria serviceRegistroAuditoria,
                                 @Value("${firmaya.invitacion.duracion-dias}") int duracionDias,
                                 @Value("${firmaya.frontend.url-acceso-externo}") String urlAcceso) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryParticipante = repositoryParticipante;
        this.repositoryInvitacion = repositoryInvitacion;
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.repositoryUsuario = repositoryUsuario;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.duracionDias = duracionDias;
        this.urlAcceso = urlAcceso;
    }

    public InvitacionDto crearYEnviar(UUID idContrato, UUID idUsuarioResponsable, SolicitudCrearInvitacion solicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);
        if (!contrato.esEditable()) {
            throw new ConflictoEstadoException("No se pueden invitar partes en el estado actual del contrato.");
        }

        String correoNormalizado = solicitud.correoElectronico().trim().toLowerCase(Locale.ROOT);
        repositoryParticipante.findByContratoIdAndCorreoNormalizado(idContrato, correoNormalizado)
                .ifPresent(existente -> {
                    throw new SolicitudInvalidaException("Esta direccion ya ha sido invitada a este contrato.");
                });

        Usuario emisor = repositoryUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        OffsetDateTime ahora = OffsetDateTime.now();
        Participante participante = Participante.crear(UUID.randomUUID(), contrato, solicitud.nombre(),
                solicitud.correoElectronico(), solicitud.rol(), ahora);
        repositoryParticipante.save(participante);

        Invitacion invitacion = Invitacion.crear(UUID.randomUUID(), contrato, participante, emisor,
                solicitud.mensaje(), ahora);
        // Se reasigna el resultado de save(): con @Id manual, save() usa merge() y la
        // instancia original queda desconectada; enviarInvitacion debe mutar la copia
        // gestionada para que marcarEnviada/marcarErrorEnvio se sincronicen con la base.
        invitacion = repositoryInvitacion.save(invitacion);

        enviarInvitacion(contrato, participante, invitacion, emisor, ahora);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(emisor, "INVITACION_CREADA", "Invitacion", "Invitacion creada para " + participante.getRolParticipacion() + ".")
                .conEntidad(invitacion.getId())
                .conContrato(idContrato));

        return aDto(invitacion);
    }

    public InvitacionDto reintentarEntrega(UUID idContrato, UUID idInvitacion, UUID idUsuarioResponsable) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);

        Invitacion invitacion = repositoryInvitacion.findByIdAndContratoId(idInvitacion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La invitacion no existe."));
        if (invitacion.getEstado() != EstadoInvitacion.ERROR_ENVIO) {
            throw new ConflictoEstadoException("Solo se puede reintentar una invitacion cuyo envio fallo.");
        }

        Usuario emisor = repositoryUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        OffsetDateTime ahora = OffsetDateTime.now();
        enviarInvitacion(contrato, invitacion.getParticipante(), invitacion, emisor, ahora);

        return aDto(invitacion);
    }

    private void enviarInvitacion(Contrato contrato, Participante participante, Invitacion invitacion,
                                   Usuario emisor, OffsetDateTime ahora) {
        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracionToken = ahora.plusDays(duracionDias);
        TokenAcceso token = TokenAcceso.deInvitacion(UUID.randomUUID(), invitacion.getId(), emisor, hash,
                ahora, expiracionToken);
        repositoryTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Fuiste invitado/a a participar del contrato \"" + contrato.getNombre()
                + "\" en FirmaYA como " + participante.getRolParticipacion() + ". "
                + (invitacion.getMensajePersonalizado() != null ? invitacion.getMensajePersonalizado() + " " : "")
                + "Accede con el siguiente enlace (valido " + duracionDias + " dias): " + enlace;
        String resumenParaRegistro = "Se envio una invitacion de acceso al contrato, valida " + duracionDias + " dias.";

        boolean enviado = serviceEnvioNotificaciones.enviarCorreoDirigidoAParticipante(contrato.getId(),
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
