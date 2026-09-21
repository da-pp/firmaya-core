package com.firmaya.api.notificaciones;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.notificaciones.dto.NotificacionPlataformaDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-20, EP-62 y EP-63: bandeja de notificaciones de plataforma del usuario autenticado. */
@Service
@Transactional
public class ServicioBandejaNotificaciones {

    private static final int TAMANO_PAGINA_DEFECTO = 20;

    private final RepositorioNotificacion repositorioNotificacion;

    public ServicioBandejaNotificaciones(RepositorioNotificacion repositorioNotificacion) {
        this.repositorioNotificacion = repositorioNotificacion;
    }

    @Transactional(readOnly = true)
    public PaginaDto<NotificacionPlataformaDto> listarMias(UUID idUsuario, Integer pagina, Boolean soloNoLeidas) {
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA_DEFECTO, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        var resultado = repositorioNotificacion.buscarDePlataforma(
                idUsuario, Boolean.TRUE.equals(soloNoLeidas), paginado);
        return PaginaDto.desde(resultado.map(NotificacionPlataformaDto::desde));
    }

    public NotificacionPlataformaDto marcarLeida(UUID idUsuario, UUID idNotificacion) {
        Notificacion notificacion = repositorioNotificacion.findByIdAndUsuarioDestinatarioId(idNotificacion, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificacion no existe."));
        notificacion.marcarLeida(OffsetDateTime.now());
        return NotificacionPlataformaDto.desde(notificacion);
    }
}
