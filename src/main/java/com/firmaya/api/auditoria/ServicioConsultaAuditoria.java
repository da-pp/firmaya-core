package com.firmaya.api.auditoria;

import com.firmaya.api.auditoria.dto.ConsultaAuditoriaDto;
import com.firmaya.api.auditoria.dto.DetalleEventoAuditoriaDto;
import com.firmaya.api.auditoria.dto.EventoAuditoriaDto;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Lectura de auditoria para el Administrador (CU-18, EP-67 y EP-68). Nunca expone
 * contrasenas, OTP ni tokens completos: EventoAuditoria nunca deberia contenerlos (ver
 * ServicioRegistroAuditoria y sus llamadores), pero ademas se sanea aqui por defecto.
 */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaAuditoria {

    private static final int TAMANO_PAGINA = 50;

    private final RepositorioEventoAuditoria repositorioEventoAuditoria;
    private final ObjectMapper objectMapper;

    public ServicioConsultaAuditoria(RepositorioEventoAuditoria repositorioEventoAuditoria, ObjectMapper objectMapper) {
        this.repositorioEventoAuditoria = repositorioEventoAuditoria;
        this.objectMapper = objectMapper;
    }

    public PaginaDto<EventoAuditoriaDto> buscar(ConsultaAuditoriaDto consulta) {
        int pagina = consulta.pagina() != null ? Math.max(consulta.pagina(), 0) : 0;
        var paginado = PageRequest.of(pagina, TAMANO_PAGINA, Sort.by(Sort.Direction.DESC, "fechaEvento"));
        var resultado = repositorioEventoAuditoria.buscar(
                aInicioDeDia(consulta.origen()),
                aFinDeDia(consulta.destino()),
                consulta.idUsuario(),
                consulta.tipoAccion(),
                consulta.idContrato(),
                paginado);
        return PaginaDto.desde(resultado.map(this::aResumen));
    }

    public DetalleEventoAuditoriaDto obtenerDetalle(UUID idEvento) {
        EventoAuditoria evento = repositorioEventoAuditoria.findById(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoException("El evento de auditoria no existe."));
        return new DetalleEventoAuditoriaDto(
                evento.getId(),
                evento.getFechaEvento(),
                describirActor(evento),
                evento.getTipoAccion(),
                evento.getTipoEntidad(),
                evento.getIdEntidad(),
                evento.getDescripcion(),
                aObjeto(evento.getDatosAnterioresJson()),
                aObjeto(evento.getDatosPosterioresJson()),
                evento.getIdVersionContrato(),
                evento.getHashSha256(),
                evento.getDireccionIp());
    }

    /** Reutilizado por {@link ServicioExportacionAuditoria} para aplicar el mismo saneo/mascara. */
    EventoAuditoriaDto aResumenPublico(EventoAuditoria evento) {
        return aResumen(evento);
    }

    private EventoAuditoriaDto aResumen(EventoAuditoria evento) {
        return new EventoAuditoriaDto(
                evento.getId(),
                evento.getFechaEvento(),
                describirActor(evento),
                evento.getTipoAccion(),
                evento.getTipoEntidad(),
                evento.getIdEntidad(),
                evento.getDescripcion(),
                enmascararIp(evento.getDireccionIp()));
    }

    private String describirActor(EventoAuditoria evento) {
        Usuario actor = evento.getUsuarioActor();
        if (actor != null) {
            return actor.getNombre() + " " + actor.getApellido();
        }
        return switch (evento.getTipoActor()) {
            case SISTEMA -> "Sistema";
            case PARTICIPANTE -> "Participante externo";
            case USUARIO -> "Usuario";
        };
    }

    private Object aObjeto(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, Object.class);
    }

    /** Politica de exposicion de IP: [PENDIENTE] en la especificacion (PD-10). Por defecto
     * se enmascara el ultimo octeto/grupo en los listados, dejando el valor completo solo
     * en el detalle individual. */
    private String enmascararIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        if (ip.contains(".")) {
            String[] partes = ip.split("\\.");
            if (partes.length == 4) {
                return partes[0] + "." + partes[1] + "." + partes[2] + ".xxx";
            }
        } else if (ip.contains(":")) {
            String[] partes = ip.split(":");
            if (partes.length > 2) {
                return partes[0] + ":" + partes[1] + ":xxxx:xxxx";
            }
        }
        return ip;
    }

    private OffsetDateTime aInicioDeDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }

    private OffsetDateTime aFinDeDia(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}
