package com.firmaya.api.contratos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioHistorialEstadoContrato extends JpaRepository<HistorialEstadoContrato, UUID> {
}
