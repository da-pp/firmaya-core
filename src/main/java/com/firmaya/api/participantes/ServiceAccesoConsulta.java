package com.firmaya.api.participantes;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.participantes.dto.EmisionEnlaceConsultaDto;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.RepositoryUsuario;
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
public class ServiceAccesoConsulta {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryParticipante repositoryParticipante;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final RepositoryUsuario repositoryUsuario;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final int duracionDias;
    private final String urlAcceso;

    public ServiceAccesoConsulta(RepositoryContrato repositoryContrato,
                                   RepositoryParticipante repositoryParticipante,
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
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.repositoryUsuario = repositoryUsuario;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.duracionDias = duracionDias;
        this.urlAcceso = urlAcceso;
    }

    public EmisionEnlaceConsultaDto emitirEnlace(UUID idContrato, UUID idParticipante, UUID idUsuarioResponsable) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuarioResponsable);
        Participante participante = repositoryParticipante.findByIdAndContratoId(idParticipante, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El participante no existe en este contrato."));
        Usuario emisor = repositoryUsuario.findById(idUsuarioResponsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        OffsetDateTime ahora = OffsetDateTime.now();
        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracion = ahora.plusDays(duracionDias);
        TokenAcceso token = TokenAcceso.deConsulta(UUID.randomUUID(), participante.getId(), emisor, hash, ahora, expiracion);
        repositoryTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Se emitio un nuevo enlace de solo consulta para el contrato \"" + contrato.getNombre()
                + "\" en FirmaYA (valido " + duracionDias + " dias): " + enlace;
        String resumen = "Se emitio un enlace renovado de consulta, valido " + duracionDias + " dias.";
        boolean enviado = serviceEnvioNotificaciones.enviarCorreoDirigidoAParticipante(idContrato, participante.getId(),
                participante.getCorreoElectronico(), "CONTRATO_INVITACION_RECIBIDA", "Enlace de consulta - FirmaYA",
                cuerpoCorreo, resumen);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(emisor, "ENLACE_CONSULTA_EMITIDO", "Participante", "Enlace de consulta renovado emitido.")
                .conEntidad(participante.getId())
                .conContrato(idContrato));

        return new EmisionEnlaceConsultaDto(participante.getId(), "CONSULTA", expiracion,
                enviado ? "ENVIADA" : "ERROR");
    }
}
