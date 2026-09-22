package com.firmaya.api.contratos;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.dto.DetalleContratoDto;
import com.firmaya.api.contratos.dto.ResumenContratoDto;
import com.firmaya.api.contratos.dto.VersionContratoDto;
import com.firmaya.api.participantes.RolParticipacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-01/CU-02/CU-04/CU-05/CU-09, EP-28 y EP-30. */
@Service
@Transactional(readOnly = true)
public class ServiceConsultaContratos {

    private static final int TAMANO_PAGINA = 20;

    private final RepositoryContrato repositoryContrato;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;

    public ServiceConsultaContratos(RepositoryContrato repositoryContrato,
                                      RepositoryVersionContrato repositoryVersionContrato,
                                      ServiceAutorizacionContrato serviceAutorizacionContrato) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
    }

    public PaginaDto<ResumenContratoDto> buscarContratosAccesibles(UUID idUsuario, EstadoContrato estado,
                                                                     String texto, Integer pagina) {
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA, Sort.by(Sort.Direction.DESC, "fechaUltimaActividad"));
        var resultado = repositoryContrato.buscarContratosAccesibles(idUsuario, estado, texto, paginado);

        List<UUID> idsVersion = resultado.getContent().stream()
                .map(Contrato::getIdVersionActual).filter(java.util.Objects::nonNull).toList();
        Map<UUID, Integer> numerosPorVersion = repositoryVersionContrato.findAllById(idsVersion).stream()
                .collect(Collectors.toMap(VersionContrato::getId, VersionContrato::getNumeroVersion));

        return PaginaDto.desde(resultado.map(contrato -> ResumenContratoDto.desde(
                contrato, numerosPorVersion.getOrDefault(contrato.getIdVersionActual(), 0))));
    }

    public DetalleContratoDto obtenerParaUsuarioInterno(UUID idContrato, UUID idUsuario) {
        Contrato contrato = obtenerContratoOLanzar(idContrato);
        boolean esResponsable = serviceAutorizacionContrato.esResponsable(contrato, idUsuario);
        Optional<RolParticipacion> rolParticipacion = serviceAutorizacionContrato
                .rolParticipacionInterna(idContrato, idUsuario);
        if (!esResponsable && rolParticipacion.isEmpty()) {
            throw new AccesoDenegadoNegocioException("No tiene acceso a este contrato.");
        }
        List<String> permisos = calcularPermisosInternos(contrato, esResponsable, rolParticipacion.orElse(null));
        return construirDetalle(contrato, permisos);
    }

    public DetalleContratoDto obtenerParaParticipanteExterno(UUID idContrato, ContextoParticipanteAutenticado contexto) {
        if (!contexto.idContrato().equals(idContrato)) {
            throw new AccesoDenegadoNegocioException("Su acceso no corresponde a este contrato.");
        }
        Contrato contrato = obtenerContratoOLanzar(idContrato);
        if (contexto.proposito() != PropositoAcceso.CONSULTA
                && (contrato.getEstado() == EstadoContrato.FIRMADO || contrato.getEstado() == EstadoContrato.ARCHIVADO)) {
            // Un enlace de invitacion/firma no habilita, por si solo, consulta indefinida tras el cierre.
            throw new AccesoDenegadoNegocioException("Este acceso ya no esta disponible; solicite un enlace de consulta.");
        }
        List<String> permisos = calcularPermisosExternos(contrato, contexto);
        return construirDetalle(contrato, permisos);
    }

    private DetalleContratoDto construirDetalle(Contrato contrato, List<String> permisos) {
        VersionContratoDto versionActual = contrato.getIdVersionActual() == null ? null
                : VersionContratoDto.desde(repositoryVersionContrato.findByIdAndContratoId(
                        contrato.getIdVersionActual(), contrato.getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("La version actual no existe.")));

        return new DetalleContratoDto(
                contrato.getId(), contrato.getNombre(), contrato.getTipoContrato(), contrato.getPartesInvolucradas(),
                contrato.getEstado(), contrato.getResponsable().getId(), contrato.getIdVersionPlantilla(),
                versionActual, contrato.getFechaInicio(), contrato.getFechaExpiracion(),
                contrato.getDescripcionPropiedad(), contrato.getFechaCreacion(), permisos);
    }

    private List<String> calcularPermisosInternos(Contrato contrato, boolean esResponsable, RolParticipacion rol) {
        List<String> permisos = new ArrayList<>();
        permisos.add("VER_HISTORIAL");
        permisos.add("VERIFICAR_INTEGRIDAD");
        boolean editable = contrato.esEditable();
        if (esResponsable) {
            if (editable) {
                permisos.add("EDITAR");
                permisos.add("INVITAR");
            }
            permisos.add("CAMBIAR_ESTADO");
            if (contrato.getEstado() == EstadoContrato.FIRMADO || contrato.getEstado() == EstadoContrato.ARCHIVADO) {
                permisos.add("DESCARGAR_FINAL");
            }
        }
        if (rol != null && rol != RolParticipacion.SOLO_LECTURA
                && (editable || contrato.getEstado() == EstadoContrato.LISTO_PARA_FIRMAR)) {
            permisos.add("COMENTAR");
        }
        if (rol == RolParticipacion.FIRMANTE && contrato.getEstado() == EstadoContrato.LISTO_PARA_FIRMAR) {
            permisos.add("FIRMAR");
        }
        if (rol != null && (contrato.getEstado() == EstadoContrato.FIRMADO || contrato.getEstado() == EstadoContrato.ARCHIVADO)) {
            permisos.add("DESCARGAR_FINAL");
        }
        return permisos;
    }

    private List<String> calcularPermisosExternos(Contrato contrato, ContextoParticipanteAutenticado contexto) {
        List<String> permisos = new ArrayList<>();
        permisos.add("VER_HISTORIAL");
        permisos.add("VERIFICAR_INTEGRIDAD");
        RolParticipacion rol = contexto.rolParticipacion();
        boolean editable = contrato.esEditable();
        if (rol != RolParticipacion.SOLO_LECTURA && (editable || contrato.getEstado() == EstadoContrato.LISTO_PARA_FIRMAR)) {
            permisos.add("COMENTAR");
        }
        if (rol == RolParticipacion.FIRMANTE && contexto.proposito() == PropositoAcceso.FIRMA
                && contrato.getEstado() == EstadoContrato.LISTO_PARA_FIRMAR) {
            permisos.add("FIRMAR");
        }
        if (contrato.getEstado() == EstadoContrato.FIRMADO || contrato.getEstado() == EstadoContrato.ARCHIVADO) {
            permisos.add("DESCARGAR_FINAL");
        }
        return permisos;
    }

    Contrato obtenerContratoOLanzar(UUID idContrato) {
        return repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
    }
}
