package com.firmaya.api.accesos;

import com.firmaya.api.accesos.dto.AccesoExternoDto;
import com.firmaya.api.accesos.dto.SesionExternaDto;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.comun.excepciones.TokenInvalidoException;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.participantes.Invitacion;
import com.firmaya.api.participantes.Participante;
import com.firmaya.api.participantes.RepositorioInvitacion;
import com.firmaya.api.participantes.RepositorioParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import com.firmaya.api.procesofirma.RepositorioSolicitudFirma;
import com.firmaya.api.procesofirma.SolicitudFirma;
import com.firmaya.api.seguridad.ServicioHashCredencial;
import com.firmaya.api.tokens.PropositoToken;
import com.firmaya.api.tokens.RepositorioTokenAcceso;
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
public class ServicioAccesoExterno {

    private final RepositorioTokenAcceso repositorioTokenAcceso;
    private final RepositorioSesionExterna repositorioSesionExterna;
    private final RepositorioInvitacion repositorioInvitacion;
    private final RepositorioParticipante repositorioParticipante;
    private final RepositorioSolicitudFirma repositorioSolicitudFirma;
    private final ServicioHashCredencial servicioHashCredencial;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final int inactividadConsultaMinutos;

    public ServicioAccesoExterno(RepositorioTokenAcceso repositorioTokenAcceso,
                                  RepositorioSesionExterna repositorioSesionExterna,
                                  RepositorioInvitacion repositorioInvitacion,
                                  RepositorioParticipante repositorioParticipante,
                                  RepositorioSolicitudFirma repositorioSolicitudFirma,
                                  ServicioHashCredencial servicioHashCredencial,
                                  ServicioRegistroAuditoria servicioRegistroAuditoria,
                                  @Value("${firmaya.accesos.inactividad-consulta-minutos}") int inactividadConsultaMinutos) {
        this.repositorioTokenAcceso = repositorioTokenAcceso;
        this.repositorioSesionExterna = repositorioSesionExterna;
        this.repositorioInvitacion = repositorioInvitacion;
        this.repositorioParticipante = repositorioParticipante;
        this.repositorioSolicitudFirma = repositorioSolicitudFirma;
        this.servicioHashCredencial = servicioHashCredencial;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.inactividadConsultaMinutos = inactividadConsultaMinutos;
    }

    public record ResultadoCanje(String credencialEnClaro, SesionExternaDto sesion) {
    }

    public ResultadoCanje canjear(String tokenPlano, String direccionIp) {
        byte[] hash = servicioHashCredencial.hashear(tokenPlano);
        TokenAcceso token = repositorioTokenAcceso.findByHashToken(hash)
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

        String credencialEnClaro = servicioHashCredencial.generarCredencialAleatoria();
        byte[] hashSesion = servicioHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracionSesion = calcularExpiracion(token, ahora);

        SesionExterna sesion = SesionExterna.crear(UUID.randomUUID(), token, participante, hashSesion, proposito,
                direccionIp, ahora, expiracionSesion);
        repositorioSesionExterna.save(sesion);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
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
        Participante participante = repositorioParticipante.findById(contexto.idParticipante())
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
        SesionExterna sesion = repositorioSesionExterna.findById(contexto.idSesionExterna())
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
        repositorioSesionExterna.findById(contexto.idSesionExterna())
                .ifPresent(sesion -> sesion.revocar(OffsetDateTime.now()));
    }

    private Participante resolverParticipante(TokenAcceso token, OffsetDateTime ahora) {
        return switch (token.getProposito()) {
            case INVITACION -> {
                Invitacion invitacion = repositorioInvitacion.findById(token.getIdInvitacion())
                        .orElseThrow(() -> new RecursoNoEncontradoException("La invitacion no existe."));
                if (invitacion.getEstado() != com.firmaya.api.participantes.EstadoInvitacion.ACEPTADA) {
                    invitacion.marcarAceptada(ahora);
                }
                yield invitacion.getParticipante();
            }
            case CONSULTA -> repositorioParticipante.findById(token.getIdParticipante())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El participante no existe."));
            case FIRMA -> {
                SolicitudFirma solicitud = repositorioSolicitudFirma.findById(token.getIdSolicitudFirma())
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
