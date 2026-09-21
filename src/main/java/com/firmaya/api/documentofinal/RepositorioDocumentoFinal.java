package com.firmaya.api.documentofinal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDocumentoFinal extends JpaRepository<DocumentoFinal, UUID> {

    Optional<DocumentoFinal> findByContratoId(UUID idContrato);
}
