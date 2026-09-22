package com.firmaya.api.participantes;

import com.firmaya.api.firma.RepositoryFirma;
import com.firmaya.api.participantes.dto.ParticipanteDto;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.RepositoryFirmanteProceso;
import com.firmaya.api.procesofirma.RepositoryProcesoFirma;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-03/CU-07, EP-37. */
@Service
@Transactional(readOnly = true)
public class ServiceConsultaParticipantes {

    private final RepositoryParticipante repositoryParticipante;
    private final RepositoryInvitacion repositoryInvitacion;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositoryFirma repositoryFirma;

    public ServiceConsultaParticipantes(RepositoryParticipante repositoryParticipante,
                                          RepositoryInvitacion repositoryInvitacion,
                                          RepositoryProcesoFirma repositoryProcesoFirma,
                                          RepositoryFirmanteProceso repositoryFirmanteProceso,
                                          RepositoryFirma repositoryFirma) {
        this.repositoryParticipante = repositoryParticipante;
        this.repositoryInvitacion = repositoryInvitacion;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositoryFirma = repositoryFirma;
    }

    public List<ParticipanteDto> listarParticipantes(UUID idContrato) {
        List<Participante> participantes = repositoryParticipante.findByContratoId(idContrato);

        Set<UUID> congelados = Set.of();
        Set<UUID> firmados = Set.of();
        var procesoVigente = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato);
        if (procesoVigente.isPresent()) {
            List<FirmanteProceso> firmantes = repositoryFirmanteProceso.findByProcesoFirmaId(procesoVigente.get().getId());
            congelados = firmantes.stream().map(f -> f.getParticipante().getId()).collect(Collectors.toSet());
            firmados = firmantes.stream()
                    .filter(f -> repositoryFirma.findByFirmanteProcesoId(f.getId()).isPresent())
                    .map(f -> f.getParticipante().getId())
                    .collect(Collectors.toSet());
        }

        Set<UUID> idsCongelados = congelados;
        Set<UUID> idsFirmados = firmados;
        return participantes.stream()
                .map(participante -> {
                    String estadoInvitacion = repositoryInvitacion.findByParticipanteId(participante.getId())
                            .map(inv -> inv.getEstado().name())
                            .orElse("SIN_INVITAR");
                    return new ParticipanteDto(
                            participante.getId(), participante.getNombre(), participante.getCorreoElectronico(),
                            participante.getRolParticipacion(), estadoInvitacion,
                            idsFirmados.contains(participante.getId()), idsCongelados.contains(participante.getId()));
                })
                .toList();
    }
}
