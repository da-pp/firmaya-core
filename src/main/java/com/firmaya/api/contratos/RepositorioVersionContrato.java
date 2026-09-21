package com.firmaya.api.contratos;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioVersionContrato extends JpaRepository<VersionContrato, UUID> {

    Optional<VersionContrato> findByIdAndContratoId(UUID id, UUID idContrato);

    Optional<VersionContrato> findTopByContratoIdOrderByNumeroVersionDesc(UUID idContrato);

    /** CU-11, EP-31: historial ordenado de la version mas reciente a la mas antigua. */
    Page<VersionContrato> findByContratoIdOrderByNumeroVersionDesc(UUID idContrato, Pageable pageable);
}
