package com.firmaya.api.plantillas;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioVersionPlantilla extends JpaRepository<VersionPlantilla, UUID> {

    Optional<VersionPlantilla> findByIdAndPlantillaId(UUID id, UUID idPlantilla);

    Optional<VersionPlantilla> findTopByPlantillaIdOrderByNumeroVersionDesc(UUID idPlantilla);

    Page<VersionPlantilla> findByPlantillaIdOrderByNumeroVersionDesc(UUID idPlantilla, Pageable pageable);
}
