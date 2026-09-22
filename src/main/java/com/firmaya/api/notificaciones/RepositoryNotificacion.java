package com.firmaya.api.notificaciones;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryNotificacion extends JpaRepository<Notificacion, UUID> {

    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.usuarioDestinatario.id = :idUsuario
              AND n.canal = com.firmaya.api.notificaciones.CanalNotificacion.PLATAFORMA
              AND (:soloNoLeidas = false OR n.fechaLectura IS NULL)
            """)
    Page<Notificacion> buscarDePlataforma(@Param("idUsuario") UUID idUsuario,
                                           @Param("soloNoLeidas") boolean soloNoLeidas,
                                           Pageable pageable);

    Optional<Notificacion> findByIdAndUsuarioDestinatarioId(UUID id, UUID idUsuario);
}
