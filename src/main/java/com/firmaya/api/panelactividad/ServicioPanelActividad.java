package com.firmaya.api.panelactividad;

import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.RepositorioVersionContrato;
import com.firmaya.api.contratos.VersionContrato;
import com.firmaya.api.contratos.dto.ResumenContratoDto;
import com.firmaya.api.panelactividad.dto.ConsultaContratosPanel;
import com.firmaya.api.panelactividad.dto.ConteoPorEstadoDto;
import com.firmaya.api.panelactividad.dto.FilaContratoPanelDto;
import com.firmaya.api.panelactividad.dto.PanelActividadDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-17, EP-64 y EP-65. Las definiciones exactas de "activo", "pendiente de firma" y
 * "proximo a vencer" son PD-09 [PENDIENTE] en la especificacion; esta clase documenta la
 * interpretacion tecnica provisional usada (ver Javadoc de cada metodo de RepositorioContrato).
 * El acceso es exclusivamente de consulta/estadisticas: nunca habilita editar contenido
 * contractual ajeno (Alternativa 4 de CU-17).
 */
@Service
@Transactional(readOnly = true)
public class ServicioPanelActividad {

    private static final int TAMANO_PAGINA = 20;
    private static final int DIAS_PERIODO_DEFECTO = 30;
    private static final int DIAS_PROXIMO_VENCIMIENTO = 7;

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;

    public ServicioPanelActividad(RepositorioContrato repositorioContrato,
                                   RepositorioVersionContrato repositorioVersionContrato) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
    }

    public PanelActividadDto obtenerResumen(LocalDate origen, LocalDate destino, List<EstadoContrato> estados) {
        LocalDate[] rango = resolverRango(origen, destino);
        OffsetDateTime inicio = inicioDeDia(rango[0]);
        OffsetDateTime fin = finDeDia(rango[1]);
        LocalDate hoy = LocalDate.now();

        long activos = repositorioContrato.contarActivos();
        long pendientesFirma = repositorioContrato.contarPendientesFirma();
        long firmadosEnPeriodo = repositorioContrato.contarFirmadosEnPeriodo(inicio, fin);
        long proximosAVencer = repositorioContrato.contarProximosAVencer(hoy, hoy.plusDays(DIAS_PROXIMO_VENCIMIENTO));

        List<ConteoPorEstadoDto> porEstado = repositorioContrato
                .contarPorEstado(inicio, fin, estados == null || estados.isEmpty() ? null : estados).stream()
                .map(fila -> new ConteoPorEstadoDto((EstadoContrato) fila[0], (Long) fila[1]))
                .toList();

        List<Contrato> recientes = repositorioContrato.findTop10ByOrderByFechaUltimaActividadDesc();
        List<ResumenContratoDto> contratosRecientes = aResumenes(recientes);

        List<String> estadosTexto = estados == null ? null : estados.stream().map(Enum::name).toList();
        return new PanelActividadDto(activos, pendientesFirma, firmadosEnPeriodo, proximosAVencer, porEstado,
                contratosRecientes, new PanelActividadDto.FiltrosAplicadosDto(rango[0], rango[1], estadosTexto));
    }

    public PaginaDto<FilaContratoPanelDto> listarContratosPorMetrica(ConsultaContratosPanel consulta) {
        if (consulta.metrica() == null) {
            throw new SolicitudInvalidaException("La metrica es obligatoria.");
        }
        LocalDate[] rango = resolverRango(consulta.origen(), consulta.destino());
        int numeroPagina = consulta.pagina() != null ? Math.max(consulta.pagina(), 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA);
        List<EstadoContrato> estados = consulta.estados() == null || consulta.estados().isEmpty() ? null : consulta.estados();

        var pagina = buscarPorMetrica(consulta.metrica(), rango, estados, paginado);
        return PaginaDto.desde(pagina.map(this::aFila));
    }

    List<Contrato> listarTodosPorMetrica(ConsultaContratosPanel consulta) {
        LocalDate[] rango = resolverRango(consulta.origen(), consulta.destino());
        List<EstadoContrato> estados = consulta.estados() == null || consulta.estados().isEmpty() ? null : consulta.estados();
        return buscarPorMetrica(consulta.metrica(), rango, estados, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
    }

    private org.springframework.data.domain.Page<Contrato> buscarPorMetrica(MetricaPanel metrica, LocalDate[] rango,
                                                                               List<EstadoContrato> estados,
                                                                               org.springframework.data.domain.Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        return switch (metrica) {
            case ACTIVOS -> repositorioContrato.buscarActivos(estados, pageable);
            case PENDIENTES_FIRMA -> repositorioContrato.buscarPendientesFirma(estados, pageable);
            case FIRMADOS_PERIODO -> repositorioContrato.buscarFirmadosEnPeriodo(
                    inicioDeDia(rango[0]), finDeDia(rango[1]), estados, pageable);
            case PROXIMOS_VENCER -> repositorioContrato.buscarProximosAVencer(
                    hoy, hoy.plusDays(DIAS_PROXIMO_VENCIMIENTO), estados, pageable);
        };
    }

    private FilaContratoPanelDto aFila(Contrato contrato) {
        return new FilaContratoPanelDto(contrato.getId(), contrato.getNombre(), contrato.getEstado().name(),
                contrato.getResponsable().getNombre() + " " + contrato.getResponsable().getApellido(),
                contrato.getFechaUltimaActividad());
    }

    private List<ResumenContratoDto> aResumenes(List<Contrato> contratos) {
        List<UUID> idsVersion = contratos.stream().map(Contrato::getIdVersionActual).filter(Objects::nonNull).toList();
        Map<UUID, Integer> numerosPorVersion = repositorioVersionContrato.findAllById(idsVersion).stream()
                .collect(Collectors.toMap(VersionContrato::getId, VersionContrato::getNumeroVersion));
        return contratos.stream()
                .map(c -> ResumenContratoDto.desde(c, numerosPorVersion.getOrDefault(c.getIdVersionActual(), 0)))
                .toList();
    }

    private LocalDate[] resolverRango(LocalDate origen, LocalDate destino) {
        if ((origen == null) != (destino == null)) {
            throw new SolicitudInvalidaException("Debe indicar ambas fechas (origen y destino) o ninguna.");
        }
        if (origen != null && !origen.isBefore(destino)) {
            throw new SolicitudInvalidaException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        if (origen != null) {
            return new LocalDate[] {origen, destino};
        }
        LocalDate hoy = LocalDate.now();
        return new LocalDate[] {hoy.minusDays(DIAS_PERIODO_DEFECTO), hoy};
    }

    private OffsetDateTime inicioDeDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }

    private OffsetDateTime finDeDia(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}
