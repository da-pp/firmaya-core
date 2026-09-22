package com.firmaya.api.procesofirma;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryFirmanteProceso extends JpaRepository<FirmanteProceso, UUID> {

    List<FirmanteProceso> findByProcesoFirmaId(UUID idProcesoFirma);

    Optional<FirmanteProceso> findByProcesoFirmaIdAndParticipanteId(UUID idProcesoFirma, UUID idParticipante);
}
