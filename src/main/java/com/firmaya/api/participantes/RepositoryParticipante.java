package com.firmaya.api.participantes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryParticipante extends JpaRepository<Participante, UUID> {

    List<Participante> findByContratoId(UUID idContrato);

    Optional<Participante> findByIdAndContratoId(UUID id, UUID idContrato);

    Optional<Participante> findByContratoIdAndCorreoNormalizado(UUID idContrato, String correoNormalizado);

    List<Participante> findByContratoIdAndRolParticipacion(UUID idContrato, RolParticipacion rol);

    Optional<Participante> findByIdAndUsuarioInternoId(UUID id, UUID idUsuarioInterno);

    boolean existsByContratoIdAndUsuarioInternoId(UUID idContrato, UUID idUsuarioInterno);
}
