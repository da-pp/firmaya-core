package com.firmaya.api.tokens;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryTokenAcceso extends JpaRepository<TokenAcceso, UUID> {

    Optional<TokenAcceso> findByHashTokenAndProposito(byte[] hashToken, PropositoToken proposito);

    Optional<TokenAcceso> findByHashToken(byte[] hashToken);

    List<TokenAcceso> findByUsuarioIdAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
            UUID idUsuario, PropositoToken proposito);

    List<TokenAcceso> findByIdInvitacionAndFechaConsumoIsNullAndFechaRevocacionIsNull(UUID idInvitacion);

    List<TokenAcceso> findByIdSolicitudFirmaAndFechaConsumoIsNullAndFechaRevocacionIsNull(UUID idSolicitudFirma);

    List<TokenAcceso> findByIdParticipanteAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
            UUID idParticipante, PropositoToken proposito);
}
