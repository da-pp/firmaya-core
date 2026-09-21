package com.firmaya.api.participantes;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioInvitacion extends JpaRepository<Invitacion, UUID> {

    Optional<Invitacion> findByIdAndContratoId(UUID id, UUID idContrato);

    Optional<Invitacion> findByParticipanteId(UUID idParticipante);
}
