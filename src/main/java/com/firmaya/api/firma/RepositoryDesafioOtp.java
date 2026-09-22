package com.firmaya.api.firma;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryDesafioOtp extends JpaRepository<DesafioOtp, UUID> {

    Optional<DesafioOtp> findByIdSolicitudFirmaAndActivoTrue(UUID idSolicitudFirma);

    Optional<DesafioOtp> findByIdAndIdSolicitudFirma(UUID id, UUID idSolicitudFirma);
}
