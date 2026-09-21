package com.firmaya.api.firma.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ContextoFirmaDto(
        UUID idSolicitud,
        UUID idContrato,
        UUID idVersion,
        int numeroVersion,
        String hashVersion,
        String nombreContrato,
        String contenido,
        LocalDate fechaLimite,
        boolean requiereAceptacion,
        boolean yaFirmado
) {
}
