package com.firmaya.api.firma;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioAceptacionFirma extends JpaRepository<AceptacionFirma, UUID> {

    Optional<AceptacionFirma> findBySolicitudFirmaId(UUID idSolicitudFirma);
}
