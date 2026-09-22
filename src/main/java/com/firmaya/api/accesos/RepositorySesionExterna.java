package com.firmaya.api.accesos;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorySesionExterna extends JpaRepository<SesionExterna, UUID> {

    @Query("""
            SELECT s FROM SesionExterna s
            JOIN FETCH s.participante p
            JOIN FETCH p.contrato
            WHERE s.hashCredencial = :hashCredencial
            """)
    Optional<SesionExterna> buscarPorHashCredencial(@Param("hashCredencial") byte[] hashCredencial);
}
