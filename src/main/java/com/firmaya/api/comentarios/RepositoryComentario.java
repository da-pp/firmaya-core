package com.firmaya.api.comentarios;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryComentario extends JpaRepository<Comentario, UUID> {

    List<Comentario> findByVersionContratoIdOrderByFechaCreacionAsc(UUID idVersionContrato);
}
