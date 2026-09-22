package com.firmaya.api.accesos;

import com.firmaya.api.accesos.dto.AccesoExternoDto;
import com.firmaya.api.accesos.dto.SesionExternaDto;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.comun.excepciones.TokenInvalidoException;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.participantes.Invitacion;
import com.firmaya.api.participantes.Participante;
import com.firmaya.api.participantes.RepositoryInvitacion;
import com.firmaya.api.participantes.RepositoryParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import com.firmaya.api.procesofirma.RepositorySolicitudFirma;
import com.firmaya.api.procesofirma.SolicitudFirma;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.PropositoToken;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-04, EP-09..EP-12: canje de token externo por sesion acotada, consulta, extension y cierre. */
@Service
@Transactional
public class ServiceAccesoExterno {

    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final RepositorySesionExterna repositorySesionExterna;
    private final RepositoryInvitacion repositoryInvitacion;
    private final RepositoryParticipante repositoryParticipante;
    private final RepositorySolicitudFirma repositorySolicitudFirma;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final int inactividadConsultaMinutos;

    public ServiceAccesoExterno(RepositoryTokenAcceso repositoryTokenAcceso,
                                  RepositorySesionExterna repositorySesionExterna,
                                  RepositoryInvitacion repositoryInvitacion,
                                  RepositoryParticipante repositoryParticipante,
                                  RepositorySolicitudFirma repositorySolicitudFirma,
                                  ServiceHashCredencial serviceHashCredencial,
                                  ServiceRegistroAuditoria serviceRegistroAuditoria,
                                  @Value("${firmaya.accesos.inactividad-consulta-minutos}") int inactividadConsultaMinutos) {
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.repositorySesionExterna = repositorySesionExterna;
        this.repositoryInvitacion = repositoryInvitacion;
        this.repositoryParticipante = repositoryParticipante;
        this.repositorySolicitudFirma = repositorySolicitudFirma;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.inactividadConsultaMinutos = inactividadConsultaMinutos;
    }

    public record ResultadoCanje(String credencialEnClaro, SesionExternaDto sesion) {
    }

    public ResultadoCanje canjear(String tokenPlano, String direccionIp) {
        byte[] hash = serviceHashCredencial.hashear(tokenPlano);
        TokenAcceso token = repositoryTokenAcceso.findByHashToken(hash)
                .orElseThrow(() -> new TokenInvalidoException("El enlace de acceso no es valido."));
        if (token.getProposito() != PropositoToken.INVITACION && token.getProposito() != PropositoToken.CONSULTA
                && token.getProposito() != PropositoToken.FIRMA) {
            throw new TokenInvalidoException("El enlace de acceso no es valido.");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!token.estaVigente(ahora)) {
            throw new TokenExpiradoException("El enlace de acceso no es valido o ha expirado.");
        }

        Participante participante = resolverParticipante(token, ahora);
        PropositoAcceso proposito = PropositoAcceso.valueOf(token.getProposito().name());

        if (proposito != PropositoAcceso.CONSULTA
                && (participante.getContrato().getEstado() == EstadoContrato.FIRMADO
                    || participante.getContrato().getEstado() == EstadoContrato.ARCHIVADO)) {
            throw new AccesoDenegadoNegocioException("Este enlace ya no habilita acceso al contrato.");
        }

        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hashSesion = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracionSesion = calcularExpiracion(token, ahora);

        SesionExterna sesion = SesionExterna.crear(UUID.randomUUID(), token, participante, hashSesion, proposito,
                direccionIp, ahora, expiracionSesion);
        repositorySesionExterna.save(sesion);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("ACCESO_EXTERNO_CANJEADO", "SesionExterna",
                        "Sesion externa creada con proposito " + proposito + ".")
                .conContrato(participante.getContrato().getId())
                .conIp(direccionIp));

        List<String> acciones = calcularAcciones(proposito, participante.getRolParticipacion(),
                participante.getContrato().getEstado());
        var dto = new SesionExternaDto(participante.getContrato().getId(), participante.getId(), proposito,
                expiracionSesion, acciones);
        return new ResultadoCanje(credencialEnClaro, dto);
    }

    @Transactional(readOnly = true)
    public AccesoExternoDto obtenerContexto(ContextoParticipanteAutenticado contexto) {
        Participante participante = repositoryParticipante.findById(contexto.idParticipante())
                .orElseThrow(() -> new RecursoNoEncontradoException("El participante no existe."));
        List<String> acciones = calcularAcciones(contexto.proposito(), participante.getRolParticipacion(),
                participante.getContrato().getEstado());
        return new AccesoExternoDto(contexto.idContrato(), contexto.idParticipante(),
                participante.getRolParticipacion(), contexto.proposito(), acciones, contexto.fechaExpiracionSesion());
    }

    public SesionExternaDto extenderSesion(ContextoParticipanteAutenticado contexto) {
        if (contexto.proposito() == PropositoAcceso.FIRMA) {
            throw new AccesoDenegadoNegocioException("Un acceso de firma no puede extenderse.");
        }
        SesionExterna sesion = repositorySesionExterna.findById(contexto.idSesionExterna())
                .orElseThrow(() -> new RecursoNoEncontradoException("La sesion no existe."));
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!sesion.estaVigente(ahora)) {
            throw new TokenExpiradoException("La sesion ha expirado.");
        }
        OffsetDateTime nuevaExpiracion = calcularExpiracion(sesion.getTokenOrigen(), ahora);
        sesion.extender(ahora, nuevaExpiracion);

        Participante participante = sesion.getParticipante();
        List<String> acciones = calcularAcciones(sesion.getProposito(), participante.getRolParticipacion(),
                participante.getContrato().getEstado());
        return new SesionExternaDto(contexto.idContrato(), contexto.idParticipante(), sesion.getProposito(),
                nuevaExpiracion, acciones);
    }

    public void cerrarSesion(ContextoParticipanteAutenticado contexto) {
        repositorySesionExterna.findById(contexto.idSesionExterna())
                .ifPresent(sesion -> sesion.revocar(OffsetDateTime.now()));
    }

    private Participante resolverParticipante(TokenAcceso token, OffsetDateTime ahora) {
        return switch (token.getProposito()) {
            case INVITACION -> {
                Invitacion invitacion = repositoryInvitacion.findById(token.getIdInvitacion())
                        .orElseThrow(() -> new RecursoNoEncontradoException("La invitacion no existe."));
                if (invitacion.getEstado() != com.firmaya.api.participantes.EstadoInvitacion.ACEPTADA) {
                    invitacion.marcarAceptada(ahora);
                }
                yield invitacion.getParticipante();
            }
            case CONSULTA -> repositoryParticipante.findById(token.getIdParticipante())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El participante no existe."));
            case FIRMA -> {
                SolicitudFirma solicitud = repositorySolicitudFirma.findById(token.getIdSolicitudFirma())
                        .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud de firma no existe."));
                yield solicitud.getFirmanteProceso().getParticipante();
            }
            default -> throw new TokenInvalidoException("El enlace de acceso no es valido.");
        };
    }

    private OffsetDateTime calcularExpiracion(TokenAcceso token, OffsetDateTime ahora) {
        OffsetDateTime limiteInactividad = ahora.plusMinutes(inactividadConsultaMinutos);
        return token.getFechaExpiracion().isBefore(limiteInactividad) ? token.getFechaExpiracion() : limiteInactividad;
    }

    private List<String> calcularAcciones(PropositoAcceso proposito, RolParticipacion rol, EstadoContrato estado) {
        List<String> acciones = new ArrayList<>();
        acciones.add("LEER");
        acciones.add("VER_HISTORIAL");
        acciones.add("VERIFICAR_INTEGRIDAD");
        boolean editable = estado == EstadoContrato.BORRADOR || estado == EstadoContrato.EN_REVISION;
        if (rol != RolParticipacion.SOLO_LECTURA && (editable || estado == EstadoContrato.LISTO_PARA_FIRMAR)) {
            acciones.add("COMENTAR");
        }
        if (proposito == PropositoAcceso.FIRMA && rol == RolParticipacion.FIRMANTE
                && estado == EstadoContrato.LISTO_PARA_FIRMAR) {
            acciones.add("FIRMAR");
        }
        if (estado == EstadoContrato.FIRMADO || estado == EstadoContrato.ARCHIVADO) {
            acciones.add("DESCARGAR_FINAL");
        }
        return acciones;
    }
}
