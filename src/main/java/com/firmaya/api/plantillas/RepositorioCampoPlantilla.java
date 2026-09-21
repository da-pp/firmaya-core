package com.firmaya.api.plantillas;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCampoPlantilla extends JpaRepository<CampoPlantilla, UUID> {

    List<CampoPlantilla> findByVersionPlantillaIdOrderByOrdenVisualAsc(UUID idVersionPlantilla);
}
