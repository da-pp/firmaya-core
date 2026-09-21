package com.firmaya.api.procesofirma;

import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.RepositorioContrato;
import com.firmaya.api.contratos.ServicioAutorizacionContrato;
import com.firmaya.api.firma.RepositorioFirma;
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
public class ServicioConsultaProcesoFirma {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioProcesoFirma repositorioProcesoFirma;
    private final RepositorioFirmanteProceso repositorioFirmanteProceso;
    private final RepositorioSolicitudFirma repositorioSolicitudFirma;
    private final RepositorioFirma repositorioFirma;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;

    public ServicioConsultaProcesoFirma(RepositorioContrato repositorioContrato,
                                         RepositorioProcesoFirma repositorioProcesoFirma,
                                         RepositorioFirmanteProceso repositorioFirmanteProceso,
                                         RepositorioSolicitudFirma repositorioSolicitudFirma,
                                         RepositorioFirma repositorioFirma,
                                         ServicioAutorizacionContrato servicioAutorizacionContrato) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioProcesoFirma = repositorioProcesoFirma;
        this.repositorioFirmanteProceso = repositorioFirmanteProceso;
        this.repositorioSolicitudFirma = repositorioSolicitudFirma;
        this.repositorioFirma = repositorioFirma;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
    }

    public Optional<DetalleProcesoFirmaDto> obtenerActualOUltimo(UUID idContrato, UUID idUsuario) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        ProcesoFirma proceso = repositorioProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .or(() -> repositorioProcesoFirma.findTopByContratoIdOrderByFechaCreacionDesc(idContrato))
                .orElse(null);
        if (proceso == null) {
            return Optional.empty();
        }

        List<FirmanteProceso> firmantes = repositorioFirmanteProceso.findByProcesoFirmaId(proceso.getId());
        List<FirmanteCongeladoDto> firmantesDto = firmantes.stream()
                .map(firmante -> {
                    var solicitud = repositorioSolicitudFirma.findByFirmanteProcesoId(firmante.getId()).orElse(null);
                    var firma = repositorioFirma.findByFirmanteProcesoId(firmante.getId()).orElse(null);
                    return new FirmanteCongeladoDto(
                            firmante.getParticipante().getId(), firmante.getNombreCongelado(),
                            firma != null ? "FIRMADO" : "PENDIENTE",
                            solicitud != null ? solicitud.getEstado().name() : "SIN_SOLICITUD",
                            firma != null ? firma.getFechaFirma() : (solicitud != null ? solicitud.getFechaUltimoEnvio() : null));
                })
                .toList();

        int completadas = (int) firmantesDto.stream().filter(f -> "FIRMADO".equals(f.estadoFirma())).count();
        var fechaLimite = repositorioSolicitudFirma.findByProcesoFirmaId(proceso.getId()).stream()
                .map(SolicitudFirma::getFechaLimite).filter(java.util.Objects::nonNull).findFirst().orElse(null);

        return Optional.of(new DetalleProcesoFirmaDto(proceso.getId(), idContrato, proceso.getEstado(),
                proceso.getIdVersionObjetivo(), proceso.getHashObjetivo(), firmantesDto, completadas,
                firmantesDto.size(), fechaLimite));
    }
}
