package com.firmaya.api.notificaciones;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryPreferenciaNotificacion extends JpaRepository<PreferenciaNotificacion, PreferenciaNotificacionId> {

    List<PreferenciaNotificacion> findByIdUsuario(UUID idUsuario);

    Optional<PreferenciaNotificacion> findByIdUsuarioAndIdTipoEvento(UUID idUsuario, UUID idTipoEvento);
}
