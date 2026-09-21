package com.firmaya.api.auditoria.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultaAuditoriaDto(
        LocalDate origen,
        LocalDate destino,
        UUID idUsuario,
        String tipoAccion,
        UUID idContrato,
        Integer pagina
) {
}
