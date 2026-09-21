package com.firmaya.api.auditoria;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioEventoAuditoria extends JpaRepository<EventoAuditoria, UUID> {

    @Query("""
            SELECT e FROM EventoAuditoria e
            WHERE (:origen IS NULL OR e.fechaEvento >= :origen)
              AND (:destino IS NULL OR e.fechaEvento <= :destino)
              AND (:idUsuario IS NULL OR e.usuarioActor.id = :idUsuario)
              AND (:tipoAccion IS NULL OR e.tipoAccion = :tipoAccion)
              AND (:idContrato IS NULL OR e.idContrato = :idContrato)
            """)
    Page<EventoAuditoria> buscar(@Param("origen") OffsetDateTime origen,
                                  @Param("destino") OffsetDateTime destino,
                                  @Param("idUsuario") UUID idUsuario,
                                  @Param("tipoAccion") String tipoAccion,
                                  @Param("idContrato") UUID idContrato,
                                  Pageable pageable);
}
