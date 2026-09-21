package com.firmaya.api.participantes;

import com.firmaya.api.firma.RepositorioFirma;
import com.firmaya.api.participantes.dto.ParticipanteDto;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.RepositorioFirmanteProceso;
import com.firmaya.api.procesofirma.RepositorioProcesoFirma;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-03/CU-07, EP-37. */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaParticipantes {

    private final RepositorioParticipante repositorioParticipante;
    private final RepositorioInvitacion repositorioInvitacion;
    private final RepositorioProcesoFirma repositorioProcesoFirma;
    private final RepositorioFirmanteProceso repositorioFirmanteProceso;
    private final RepositorioFirma repositorioFirma;

    public ServicioConsultaParticipantes(RepositorioParticipante repositorioParticipante,
                                          RepositorioInvitacion repositorioInvitacion,
                                          RepositorioProcesoFirma repositorioProcesoFirma,
                                          RepositorioFirmanteProceso repositorioFirmanteProceso,
                                          RepositorioFirma repositorioFirma) {
        this.repositorioParticipante = repositorioParticipante;
        this.repositorioInvitacion = repositorioInvitacion;
        this.repositorioProcesoFirma = repositorioProcesoFirma;
        this.repositorioFirmanteProceso = repositorioFirmanteProceso;
        this.repositorioFirma = repositorioFirma;
    }

    public List<ParticipanteDto> listarParticipantes(UUID idContrato) {
        List<Participante> participantes = repositorioParticipante.findByContratoId(idContrato);

        Set<UUID> congelados = Set.of();
        Set<UUID> firmados = Set.of();
        var procesoVigente = repositorioProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato);
        if (procesoVigente.isPresent()) {
            List<FirmanteProceso> firmantes = repositorioFirmanteProceso.findByProcesoFirmaId(procesoVigente.get().getId());
            congelados = firmantes.stream().map(f -> f.getParticipante().getId()).collect(Collectors.toSet());
            firmados = firmantes.stream()
                    .filter(f -> repositorioFirma.findByFirmanteProcesoId(f.getId()).isPresent())
                    .map(f -> f.getParticipante().getId())
                    .collect(Collectors.toSet());
        }

        Set<UUID> idsCongelados = congelados;
        Set<UUID> idsFirmados = firmados;
        return participantes.stream()
                .map(participante -> {
                    String estadoInvitacion = repositorioInvitacion.findByParticipanteId(participante.getId())
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
