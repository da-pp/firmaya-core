package com.firmaya.api.procesofirma;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioSolicitudFirma extends JpaRepository<SolicitudFirma, UUID> {

    List<SolicitudFirma> findByProcesoFirmaId(UUID idProcesoFirma);

    Optional<SolicitudFirma> findByIdAndProcesoFirmaId(UUID id, UUID idProcesoFirma);

    Optional<SolicitudFirma> findByFirmanteProcesoId(UUID idFirmanteProceso);
}
