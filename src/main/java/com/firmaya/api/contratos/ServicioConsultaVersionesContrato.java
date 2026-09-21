package com.firmaya.api.contratos;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.dto.ResumenVersionContratoDto;
import com.firmaya.api.contratos.dto.VersionContratoDto;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-11, EP-31 y EP-32: historial de versiones y contenido de una version puntual, siempre en
 * solo lectura. Accesible a Responsable, Firmante, Revisor y Solo lectura (B02): se reutiliza
 * el mismo criterio de acceso minimo de ServicioAutorizacionContrato (cualquier participacion
 * habilita "Ver"), tal como documenta la Alternativa 3 de CU-11 para roles restringidos solo
 * en Comparar/Restaurar, no en la consulta de historial.
 */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaVersionesContrato {

    private static final int TAMANO_PAGINA = 20;

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;

    public ServicioConsultaVersionesContrato(RepositorioContrato repositorioContrato,
                                              RepositorioVersionContrato repositorioVersionContrato,
                                              ServicioAutorizacionContrato servicioAutorizacionContrato) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
    }

    public PaginaDto<ResumenVersionContratoDto> listarHistorialParaUsuarioInterno(UUID idContrato, UUID idUsuario,
                                                                                    Integer pagina) {
        Contrato contrato = obtenerContrato(idContrato);
        servicioAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return listarHistorial(contrato, pagina);
    }

    public PaginaDto<ResumenVersionContratoDto> listarHistorialParaParticipanteExterno(UUID idContrato,
                                                                                         ContextoParticipanteAutenticado contexto,
                                                                                         Integer pagina) {
        Contrato contrato = obtenerContrato(idContrato);
        verificarContratoDelContexto(idContrato, contexto);
        return listarHistorial(contrato, pagina);
    }

    private PaginaDto<ResumenVersionContratoDto> listarHistorial(Contrato contrato, Integer pagina) {
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA);
        var resultado = repositorioVersionContrato.findByContratoIdOrderByNumeroVersionDesc(contrato.getId(), paginado);
        return PaginaDto.desde(resultado.map(version ->
                ResumenVersionContratoDto.desde(version, version.getId().equals(contrato.getIdVersionActual()))));
    }

    public VersionContratoDto obtenerVersionParaUsuarioInterno(UUID idContrato, UUID idVersion, UUID idUsuario) {
        Contrato contrato = obtenerContrato(idContrato);
        servicioAutorizacionContrato.verificarAccesoLectura(contrato, idUsuario);
        return obtenerVersion(idContrato, idVersion);
    }

    public VersionContratoDto obtenerVersionParaParticipanteExterno(UUID idContrato, UUID idVersion,
                                                                      ContextoParticipanteAutenticado contexto) {
        verificarContratoDelContexto(idContrato, contexto);
        return obtenerVersion(idContrato, idVersion);
    }

    private VersionContratoDto obtenerVersion(UUID idContrato, UUID idVersion) {
        VersionContrato version = repositorioVersionContrato.findByIdAndContratoId(idVersion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version no existe para este contrato."));
        return VersionContratoDto.desde(version);
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
}
