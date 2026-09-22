package com.firmaya.api.procesofirma;

import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.firma.RepositoryFirma;
import com.firmaya.api.procesofirma.dto.DetalleProcesoFirmaDto;
import com.firmaya.api.procesofirma.dto.FirmanteCongeladoDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-07/CU-09, EP-46. */
@Service
@Transactional(readOnly = true)
public class ServiceConsultaProcesoFirma {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositorySolicitudFirma repositorySolicitudFirma;
    private final RepositoryFirma repositoryFirma;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;

    public ServiceConsultaProcesoFirma(RepositoryContrato repositoryContrato,
                                         RepositoryProcesoFirma repositoryProcesoFirma,
                                         RepositoryFirmanteProceso repositoryFirmanteProceso,
                                         RepositorySolicitudFirma repositorySolicitudFirma,
                                         RepositoryFirma repositoryFirma,
                                         ServiceAutorizacionContrato serviceAutorizacionContrato) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositorySolicitudFirma = repositorySolicitudFirma;
        this.repositoryFirma = repositoryFirma;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
    }

    public Optional<DetalleProcesoFirmaDto> obtenerActualOUltimo(UUID idContrato, UUID idUsuario) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .or(() -> repositoryProcesoFirma.findTopByContratoIdOrderByFechaCreacionDesc(idContrato))
                .orElse(null);
        if (proceso == null) {
            return Optional.empty();
        }

        List<FirmanteProceso> firmantes = repositoryFirmanteProceso.findByProcesoFirmaId(proceso.getId());
        List<FirmanteCongeladoDto> firmantesDto = firmantes.stream()
                .map(firmante -> {
                    var solicitud = repositorySolicitudFirma.findByFirmanteProcesoId(firmante.getId()).orElse(null);
                    var firma = repositoryFirma.findByFirmanteProcesoId(firmante.getId()).orElse(null);
                    return new FirmanteCongeladoDto(
                            firmante.getParticipante().getId(), firmante.getNombreCongelado(),
                            firma != null ? "FIRMADO" : "PENDIENTE",
                            solicitud != null ? solicitud.getEstado().name() : "SIN_SOLICITUD",
                            firma != null ? firma.getFechaFirma() : (solicitud != null ? solicitud.getFechaUltimoEnvio() : null));
                })
                .toList();

        int completadas = (int) firmantesDto.stream().filter(f -> "FIRMADO".equals(f.estadoFirma())).count();
        var fechaLimite = repositorySolicitudFirma.findByProcesoFirmaId(proceso.getId()).stream()
                .map(SolicitudFirma::getFechaLimite).filter(java.util.Objects::nonNull).findFirst().orElse(null);

        return Optional.of(new DetalleProcesoFirmaDto(proceso.getId(), idContrato, proceso.getEstado(),
                proceso.getIdVersionObjetivo(), proceso.getHashObjetivo(), firmantesDto, completadas,
                firmantesDto.size(), fechaLimite));
    }
}
