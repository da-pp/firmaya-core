package com.firmaya.api.contratos;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.accesos.PropositoAcceso;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.dto.ComparacionVersionesDto;
import com.firmaya.api.contratos.dto.DiferenciaDto;
import com.firmaya.api.contratos.dto.DiferenciaDto.TipoDiferencia;
import com.firmaya.api.contratos.dto.ResumenVersionContratoDto;
import com.firmaya.api.participantes.RolParticipacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-12, EP-34: compara dos versiones historicas del mismo contrato sin modificarlas.
 * Autorizacion B02: solo Responsable o Revisor comparan (Alternativa 3: Firmante/Solo lectura
 * quedan fuera). Granularidad de diferencias: PD-07 [PENDIENTE] en la especificacion; esta
 * implementacion compara por linea (separador `\n`) mediante subsecuencia comun mas larga,
 * decision tecnica provisional documentada aqui para revision.
 */
@Service
@Transactional(readOnly = true)
public class ServicioComparacionContratos {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;

    public ServicioComparacionContratos(RepositorioContrato repositorioContrato,
                                         RepositorioVersionContrato repositorioVersionContrato,
                                         ServicioAutorizacionContrato servicioAutorizacionContrato) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
    }

    public ComparacionVersionesDto compararParaUsuarioInterno(UUID idContrato, UUID idUsuario,
                                                                UUID idVersionOrigen, UUID idVersionDestino) {
        Contrato contrato = obtenerContrato(idContrato);
        boolean esResponsable = servicioAutorizacionContrato.esResponsable(contrato, idUsuario);
        Optional<RolParticipacion> rol = servicioAutorizacionContrato.rolParticipacionInterna(idContrato, idUsuario);
        if (!esResponsable && rol.map(r -> r != RolParticipacion.REVISOR).orElse(true)) {
            throw new AccesoDenegadoNegocioException("Solo el responsable o un revisor autorizado pueden comparar versiones.");
        }
        return comparar(idContrato, idVersionOrigen, idVersionDestino);
    }

    public ComparacionVersionesDto compararParaParticipanteExterno(UUID idContrato,
                                                                      ContextoParticipanteAutenticado contexto,
                                                                      UUID idVersionOrigen, UUID idVersionDestino) {
        if (!contexto.idContrato().equals(idContrato)) {
            throw new AccesoDenegadoNegocioException("Su acceso no corresponde a este contrato.");
        }
        if (contexto.rolParticipacion() != RolParticipacion.REVISOR || contexto.proposito() == PropositoAcceso.FIRMA) {
            throw new AccesoDenegadoNegocioException("Solo un revisor autorizado puede comparar versiones.");
        }
        return comparar(idContrato, idVersionOrigen, idVersionDestino);
    }

    private ComparacionVersionesDto comparar(UUID idContrato, UUID idVersionOrigen, UUID idVersionDestino) {
        if (idVersionOrigen.equals(idVersionDestino)) {
            throw new SolicitudInvalidaException("Seleccione dos versiones diferentes para realizar la comparacion.");
        }
        Contrato contrato = obtenerContrato(idContrato);
        VersionContrato origen = obtenerVersion(idContrato, idVersionOrigen);
        VersionContrato destino = obtenerVersion(idContrato, idVersionDestino);

        List<DiferenciaDto> cambios = calcularDiferencias(origen.getContenido(), destino.getContenido());
        int cantidadCambios = (int) cambios.stream().filter(d -> d.tipo() != TipoDiferencia.SIN_CAMBIO).count();

        return new ComparacionVersionesDto(
                ResumenVersionContratoDto.desde(origen, origen.getId().equals(contrato.getIdVersionActual())),
                ResumenVersionContratoDto.desde(destino, destino.getId().equals(contrato.getIdVersionActual())),
                cambios,
                cantidadCambios);
    }

    private List<DiferenciaDto> calcularDiferencias(String contenidoOrigen, String contenidoDestino) {
        String[] a = contenidoOrigen.split("\n", -1);
        String[] b = contenidoDestino.split("\n", -1);
        int n = a.length;
        int m = b.length;
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = a[i].equals(b[j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        List<DiferenciaDto> resultado = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (a[i].equals(b[j])) {
                resultado.add(new DiferenciaDto(TipoDiferencia.SIN_CAMBIO, a[i]));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                resultado.add(new DiferenciaDto(TipoDiferencia.ELIMINADO, a[i]));
                i++;
            } else {
                resultado.add(new DiferenciaDto(TipoDiferencia.AGREGADO, b[j]));
                j++;
            }
        }
        while (i < n) {
            resultado.add(new DiferenciaDto(TipoDiferencia.ELIMINADO, a[i]));
            i++;
        }
        while (j < m) {
            resultado.add(new DiferenciaDto(TipoDiferencia.AGREGADO, b[j]));
            j++;
        }
        return resultado;
    }

    private VersionContrato obtenerVersion(UUID idContrato, UUID idVersion) {
        return repositorioVersionContrato.findByIdAndContratoId(idVersion, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version no existe para este contrato."));
    }

    private Contrato obtenerContrato(UUID idContrato) {
        return repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
    }
}
