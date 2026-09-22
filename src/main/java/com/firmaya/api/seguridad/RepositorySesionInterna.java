package com.firmaya.api.seguridad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorySesionInterna extends JpaRepository<SesionInterna, UUID> {

    /**
     * JOIN FETCH del usuario: el filtro de autenticacion se ejecuta fuera de una
     * transaccion/sesion Hibernate abierta (open-in-view=false), asi que el acceso a
     * sesion.getUsuario() debe resolverse en esta misma consulta para evitar
     * LazyInitializationException.
     */
    @Query("SELECT s FROM SesionInterna s JOIN FETCH s.usuario WHERE s.hashCredencial = :hashCredencial")
    Optional<SesionInterna> findByHashCredencial(@Param("hashCredencial") byte[] hashCredencial);

    /** CU-15, EP-17: al desactivar una cuenta se invalidan todas sus sesiones vigentes. */
    @Query("SELECT s FROM SesionInterna s WHERE s.usuario.id = :idUsuario AND s.fechaRevocacion IS NULL")
    List<SesionInterna> findVigentesPorUsuario(@Param("idUsuario") UUID idUsuario);
}
