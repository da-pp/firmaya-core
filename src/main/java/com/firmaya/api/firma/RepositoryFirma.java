package com.firmaya.api.firma;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryFirma extends JpaRepository<Firma, UUID> {

    Optional<Firma> findBySolicitudFirmaId(UUID idSolicitudFirma);

    Optional<Firma> findByFirmanteProcesoId(UUID idFirmanteProceso);

    List<Firma> findByProcesoFirmaId(UUID idProcesoFirma);

    long countByProcesoFirmaId(UUID idProcesoFirma);
}
