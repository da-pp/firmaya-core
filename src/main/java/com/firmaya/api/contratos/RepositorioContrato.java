package com.firmaya.api.contratos;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioContrato extends JpaRepository<Contrato, UUID> {

    /**
     * Contratos accesibles por un usuario interno: como Responsable o como participante
     * interno (Participante.id_usuario_interno). Filtro de estado y texto opcionales.
     */
    @Query("""
            SELECT DISTINCT c FROM Contrato c
            LEFT JOIN Participante p ON p.contrato = c AND p.usuarioInterno.id = :idUsuario
            WHERE (c.responsable.id = :idUsuario OR p.id IS NOT NULL)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
            """)
    Page<Contrato> buscarContratosAccesibles(@Param("idUsuario") UUID idUsuario,
                                              @Param("estado") EstadoContrato estado,
                                              @Param("texto") String texto,
                                              Pageable pageable);

    /* ===== CU-17, EP-64..EP-66: panel de actividad global (metricas provisionales, PD-09). ===== */

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.estado <> 'ARCHIVADO'")
    long contarActivos();

    @Query("SELECT COUNT(DISTINCT c) FROM Contrato c JOIN ProcesoFirma pf ON pf.contrato = c "
            + "WHERE pf.finalizado = false AND c.estado = 'LISTO_PARA_FIRMAR'")
    long contarPendientesFirma();

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaFirma BETWEEN :inicio AND :fin")
    long contarFirmadosEnPeriodo(@Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaExpiracion IS NOT NULL "
            + "AND c.fechaExpiracion BETWEEN :hoy AND :limite AND c.estado <> 'ARCHIVADO'")
    long contarProximosAVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    @Query("""
            SELECT c.estado, COUNT(c) FROM Contrato c
            WHERE c.fechaCreacion BETWEEN :inicio AND :fin
              AND (:estados IS NULL OR c.estado IN :estados)
            GROUP BY c.estado
            """)
    List<Object[]> contarPorEstado(@Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin,
                                    @Param("estados") List<EstadoContrato> estados);

    List<Contrato> findTop10ByOrderByFechaUltimaActividadDesc();

    @Query("SELECT c FROM Contrato c WHERE c.estado <> 'ARCHIVADO' AND (:estados IS NULL OR c.estado IN :estados)")
    Page<Contrato> buscarActivos(@Param("estados") List<EstadoContrato> estados, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Contrato c JOIN ProcesoFirma pf ON pf.contrato = c "
            + "WHERE pf.finalizado = false AND c.estado = 'LISTO_PARA_FIRMAR' "
            + "AND (:estados IS NULL OR c.estado IN :estados)")
    Page<Contrato> buscarPendientesFirma(@Param("estados") List<EstadoContrato> estados, Pageable pageable);

    @Query("SELECT c FROM Contrato c WHERE c.fechaFirma BETWEEN :inicio AND :fin "
            + "AND (:estados IS NULL OR c.estado IN :estados)")
    Page<Contrato> buscarFirmadosEnPeriodo(@Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin,
                                            @Param("estados") List<EstadoContrato> estados, Pageable pageable);

    @Query("SELECT c FROM Contrato c WHERE c.fechaExpiracion IS NOT NULL "
            + "AND c.fechaExpiracion BETWEEN :hoy AND :limite AND c.estado <> 'ARCHIVADO' "
            + "AND (:estados IS NULL OR c.estado IN :estados)")
    Page<Contrato> buscarProximosAVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite,
                                          @Param("estados") List<EstadoContrato> estados, Pageable pageable);
}
