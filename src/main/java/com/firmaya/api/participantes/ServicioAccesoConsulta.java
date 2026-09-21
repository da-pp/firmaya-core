package com.firmaya.api.participantes;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.ServicioAutorizacionContrato;
import com.firmaya.api.notificaciones.ServicioEnvioNotificaciones;
import com.firmaya.api.participantes.dto.EmisionEnlaceConsultaDto;
import com.firmaya.api.seguridad.ServicioHashCredencial;
import com.firmaya.api.tokens.RepositorioTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-03/CU-04, EP-40: emite un enlace renovado de solo consulta a un participante ya
 * existente (nunca crea participantes nuevos), util en particular tras Firmado/Archivado.
 */
@Service
@Transactional
public class ServicioAccesoConsulta {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioParticipante repositorioParticipante;
    private final RepositorioTokenAcceso repositorioTokenAcceso;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;
    private final ServicioHashCredencial servicioHashCredencial;
    private final ServicioEnvioNotificaciones servicioEnvioNotificaciones;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final int duracionDias;
    private final String urlAcceso;

    public ServicioAccesoConsulta(RepositorioContrato repositorioContrato,
                                   RepositorioParticipante repositorioParticipante,
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
        this.repositorioTokenAcceso = repositorioTokenAcceso;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
        this.servicioHashCredencial = servicioHashCredencial;
        this.servicioEnvioNotificaciones = servicioEnvioNotificaciones;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.duracionDias = duracionDias;
        this.urlAcceso = urlAcceso;
    }

    public EmisionEnlaceConsultaDto emitirEnlace(UUID idContrato, UUID idParticipante, UUID idUsuarioResponsable) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);
        Participante participante = repositorioParticipante.findByIdAndContratoId(idParticipante, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El participante no existe en este contrato."));
        Usuario emisor = repositorioUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        OffsetDateTime ahora = OffsetDateTime.now();
        String credencialEnClaro = servicioHashCredencial.generarCredencialAleatoria();
        byte[] hash = servicioHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracion = ahora.plusDays(duracionDias);
        TokenAcceso token = TokenAcceso.deConsulta(UUID.randomUUID(), participante.getId(), emisor, hash, ahora, expiracion);
        repositorioTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Se emitio un nuevo enlace de solo consulta para el contrato \"" + contrato.getNombre()
                + "\" en FirmaYA (valido " + duracionDias + " dias): " + enlace;
        String resumen = "Se emitio un enlace renovado de consulta, valido " + duracionDias + " dias.";
        boolean enviado = servicioEnvioNotificaciones.enviarCorreoDirigidoAParticipante(idContrato, participante.getId(),
                participante.getCorreoElectronico(), "CONTRATO_INVITACION_RECIBIDA", "Enlace de consulta - FirmaYA",
                cuerpoCorreo, resumen);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(emisor, "ENLACE_CONSULTA_EMITIDO", "Participante", "Enlace de consulta renovado emitido.")
                .conEntidad(participante.getId())
                .conContrato(idContrato));

        return new EmisionEnlaceConsultaDto(participante.getId(), "CONSULTA", expiracion,
                enviado ? "ENVIADA" : "ERROR");
    }
}
