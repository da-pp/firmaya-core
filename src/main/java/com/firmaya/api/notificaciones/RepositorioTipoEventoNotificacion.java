package com.firmaya.api.notificaciones;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioTipoEventoNotificacion extends JpaRepository<TipoEventoNotificacion, UUID> {

    List<TipoEventoNotificacion> findByActivoTrue();

    Optional<TipoEventoNotificacion> findByCodigoAndActivoTrue(String codigo);
}
