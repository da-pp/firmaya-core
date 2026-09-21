package com.firmaya.api.plantillas;

import com.firmaya.api.contratos.TipoContrato;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioPlantilla extends JpaRepository<Plantilla, UUID> {

    List<Plantilla> findByEstadoAndTipoContrato(EstadoPlantilla estado, TipoContrato tipoContrato);

    List<Plantilla> findByEstado(EstadoPlantilla estado);

    /** CU-16, EP-21: filtros opcionales por tipo y estado; incluye activas e inactivas. */
    @Query("""
            SELECT p FROM Plantilla p
            WHERE (:tipo IS NULL OR p.tipoContrato = :tipo)
              AND (:estado IS NULL OR p.estado = :estado)
            """)
    Page<Plantilla> buscarPlantillas(@Param("tipo") TipoContrato tipo,
                                      @Param("estado") EstadoPlantilla estado,
                                      Pageable pageable);
}
