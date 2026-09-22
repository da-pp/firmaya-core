package com.firmaya.api.notificaciones;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryCanalPreferidoUsuario extends JpaRepository<CanalPreferidoUsuario, CanalPreferidoUsuarioId> {

    List<CanalPreferidoUsuario> findByIdUsuario(UUID idUsuario);

    void deleteByIdUsuario(UUID idUsuario);
}
