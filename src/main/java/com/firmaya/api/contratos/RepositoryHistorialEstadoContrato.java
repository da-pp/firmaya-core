package com.firmaya.api.contratos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryHistorialEstadoContrato extends JpaRepository<HistorialEstadoContrato, UUID> {
}
