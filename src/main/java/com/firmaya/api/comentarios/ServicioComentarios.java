package com.firmaya.api.comentarios;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comentarios.dto.ComentarioDto;
import com.firmaya.api.comentarios.dto.SolicitudCrearComentario;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.RepositorioVersionContrato;
import com.firmaya.api.contratos.ServicioAutorizacionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.participantes.RepositorioParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import com.firmaya.api.usuarios.RepositorioUsuario;
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
public class ServicioComentarios {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final RepositorioComentario repositorioComentario;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioParticipante repositorioParticipante;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;

    public ServicioComentarios(RepositorioContrato repositorioContrato,
                                RepositorioVersionContrato repositorioVersionContrato,
                                RepositorioComentario repositorioComentario,
                                RepositorioUsuario repositorioUsuario,
                                RepositorioParticipante repositorioParticipante,
                                ServicioAutorizacionContrato servicioAutorizacionContrato,
                                ServicioRegistroAuditoria servicioRegistroAuditoria) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.repositorioComentario = repositorioComentario;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioParticipante = repositorioParticipante;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listarPorVersionParaUsuarioInterno(UUID idContrato, UUID idVersion, UUID idUsuario) {
        Contrato contrato = obtenerContrato(idContrato);
        servicioAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return listar(idContrato, idVersion);
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listarPorVersionParaParticipanteExterno(UUID idContrato, UUID idVersion,
                                                                         ContextoParticipanteAutenticado contexto) {
        verificarContratoDelContexto(idContrato, contexto);
        return listar(idContrato, idVersion);
    }

    private List<ComentarioDto> listar(UUID idContrato, UUID idVersion) {
        VersionContrato version = repositorioVersionContrato.findByIdAndContratoId(idVersion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version no existe para este contrato."));
        return repositorioComentario.findByVersionContratoIdOrderByFechaCreacionAsc(version.getId()).stream()
                .map(this::aDto)
                .toList();
    }

    public ComentarioDto publicarComoUsuarioInterno(UUID idContrato, UUID idVersion, UUID idUsuario,
                                                      SolicitudCrearComentario solicitud) {
        Contrato contrato = obtenerContrato(idContrato);
        boolean esResponsable = servicioAutorizacionContrato.esResponsable(contrato, idUsuario);
        Optional<RolParticipacion> rol = servicioAutorizacionContrato.rolParticipacionInterna(idContrato, idUsuario);
        if (!esResponsable && rol.isEmpty()) {
            throw new AccesoDenegadoNegocioException("No tiene acceso a este contrato.");
        }
        if (rol.isPresent() && rol.get() == RolParticipacion.SOLO_LECTURA) {
            throw new AccesoDenegadoNegocioException("No puede anadir comentarios con su rol actual.");
        }
        VersionContrato version = verificarVersionComentable(contrato, idVersion);

        Usuario autor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        Comentario comentario = Comentario.deUsuario(UUID.randomUUID(), version, autor, solicitud.texto(),
                solicitud.textoSeleccionado(), null, OffsetDateTime.now());
        repositorioComentario.save(comentario);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
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
        repositorioComentario.save(comentario);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("COMENTARIO_PUBLICADO", "Comentario", "Comentario publicado por un participante externo.")
                .conVersionContrato(idContrato, version.getId()));

        return aDto(comentario);
    }

    private VersionContrato verificarVersionComentable(Contrato contrato, UUID idVersion) {
        if (contrato.getEstado() != EstadoContrato.BORRADOR && contrato.getEstado() != EstadoContrato.EN_REVISION
                && contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("No se pueden publicar comentarios en el estado actual del contrato.");
        }
        VersionContrato version = repositorioVersionContrato.findByIdAndContratoId(idVersion, contrato.getId())
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
        return repositorioContrato.findById(idContrato)
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
        return repositorioParticipante.findById(idParticipante).map(p -> p.getNombre()).orElse("Participante externo");
    }
}
