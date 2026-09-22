package com.firmaya.api.comentarios;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comentarios.dto.ComentarioDto;
import com.firmaya.api.comentarios.dto.SolicitudCrearComentario;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.RepositoryVersionContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.participantes.RepositoryParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import com.firmaya.api.usuarios.RepositoryUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-06, EP-41 y EP-42. */
@Service
@Transactional
public class ServiceComentarios {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final RepositoryComentario repositoryComentario;
    private final RepositoryUsuario repositoryUsuario;
    private final RepositoryParticipante repositoryParticipante;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceComentarios(RepositoryContrato repositoryContrato,
                                RepositoryVersionContrato repositoryVersionContrato,
                                RepositoryComentario repositoryComentario,
                                RepositoryUsuario repositoryUsuario,
                                RepositoryParticipante repositoryParticipante,
                                ServiceAutorizacionContrato serviceAutorizacionContrato,
                                ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.repositoryComentario = repositoryComentario;
        this.repositoryUsuario = repositoryUsuario;
        this.repositoryParticipante = repositoryParticipante;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listarPorVersionParaUsuarioInterno(UUID idContrato, UUID idVersion, UUID idUsuario) {
        Contrato contrato = obtenerContrato(idContrato);
        serviceAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return listar(idContrato, idVersion);
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listarPorVersionParaParticipanteExterno(UUID idContrato, UUID idVersion,
                                                                         ContextoParticipanteAutenticado contexto) {
        verificarContratoDelContexto(idContrato, contexto);
        return listar(idContrato, idVersion);
    }

    private List<ComentarioDto> listar(UUID idContrato, UUID idVersion) {
        VersionContrato version = repositoryVersionContrato.findByIdAndContratoId(idVersion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version no existe para este contrato."));
        return repositoryComentario.findByVersionContratoIdOrderByFechaCreacionAsc(version.getId()).stream()
                .map(this::aDto)
                .toList();
    }

    public ComentarioDto publicarComoUsuarioInterno(UUID idContrato, UUID idVersion, UUID idUsuario,
                                                      SolicitudCrearComentario solicitud) {
        Contrato contrato = obtenerContrato(idContrato);
        boolean esResponsable = serviceAutorizacionContrato.esResponsable(contrato, idUsuario);
        Optional<RolParticipacion> rol = serviceAutorizacionContrato.rolParticipacionInterna(idContrato, idUsuario);
        if (!esResponsable && rol.isEmpty()) {
            throw new AccesoDenegadoNegocioException("No tiene acceso a este contrato.");
        }
        if (rol.isPresent() && rol.get() == RolParticipacion.SOLO_LECTURA) {
            throw new AccesoDenegadoNegocioException("No puede anadir comentarios con su rol actual.");
        }
        VersionContrato version = verificarVersionComentable(contrato, idVersion);

        Usuario autor = repositoryUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        Comentario comentario = Comentario.deUsuario(UUID.randomUUID(), version, autor, solicitud.texto(),
                solicitud.textoSeleccionado(), null, OffsetDateTime.now());
        repositoryComentario.save(comentario);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(autor, "COMENTARIO_PUBLICADO", "Comentario", "Comentario publicado en una version del contrato.")
                .conVersionContrato(idContrato, version.getId()));

        return aDto(comentario);
    }

    public ComentarioDto publicarComoParticipanteExterno(UUID idContrato, UUID idVersion,
                                                           ContextoParticipanteAutenticado contexto,
                                                           SolicitudCrearComentario solicitud) {
        verificarContratoDelContexto(idContrato, contexto);
        if (contexto.rolParticipacion() == RolParticipacion.SOLO_LECTURA) {
            throw new AccesoDenegadoNegocioException("No puede anadir comentarios con su rol actual.");
        }
        Contrato contrato = obtenerContrato(idContrato);
        VersionContrato version = verificarVersionComentable(contrato, idVersion);

        Comentario comentario = Comentario.deParticipante(UUID.randomUUID(), version, contexto.idParticipante(),
                solicitud.texto(), solicitud.textoSeleccionado(), null, OffsetDateTime.now());
        repositoryComentario.save(comentario);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("COMENTARIO_PUBLICADO", "Comentario", "Comentario publicado por un participante externo.")
                .conVersionContrato(idContrato, version.getId()));

        return aDto(comentario);
    }

    private VersionContrato verificarVersionComentable(Contrato contrato, UUID idVersion) {
        if (contrato.getEstado() != EstadoContrato.BORRADOR && contrato.getEstado() != EstadoContrato.EN_REVISION
                && contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("No se pueden publicar comentarios en el estado actual del contrato.");
        }
        VersionContrato version = repositoryVersionContrato.findByIdAndContratoId(idVersion, contrato.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La version no existe para este contrato."));
        if (contrato.getEstado() == EstadoContrato.LISTO_PARA_FIRMAR
                && !version.getId().equals(contrato.getIdVersionActual())) {
            throw new ConflictoEstadoException("Solo se puede comentar la version congelada vigente.");
        }
        return version;
    }

    private void verificarContratoDelContexto(UUID idContrato, ContextoParticipanteAutenticado contexto) {
        if (!contexto.idContrato().equals(idContrato)) {
            throw new AccesoDenegadoNegocioException("Su acceso no corresponde a este contrato.");
        }
    }

    private Contrato obtenerContrato(UUID idContrato) {
        return repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
    }

    private ComentarioDto aDto(Comentario comentario) {
        String nombreAutor = comentario.getUsuarioAutor() != null
                ? comentario.getUsuarioAutor().getNombre() + " " + comentario.getUsuarioAutor().getApellido()
                : nombreParticipante(comentario.getIdParticipanteAutor());
        return new ComentarioDto(comentario.getId(), comentario.getVersionContrato().getId(), nombreAutor,
                comentario.getTexto(), comentario.getTextoSeleccionado(), comentario.getFechaCreacion());
    }

    private String nombreParticipante(UUID idParticipante) {
        return repositoryParticipante.findById(idParticipante).map(p -> p.getNombre()).orElse("Participante externo");
    }
}
