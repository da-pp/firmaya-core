package com.firmaya.api.procesofirma;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioProcesoFirma extends JpaRepository<ProcesoFirma, UUID> {

    Optional<ProcesoFirma> findByContratoIdAndFinalizadoFalse(UUID idContrato);

    Optional<ProcesoFirma> findTopByContratoIdOrderByFechaCreacionDesc(UUID idContrato);

    Optional<ProcesoFirma> findByIdAndContratoId(UUID id, UUID idContrato);
}
