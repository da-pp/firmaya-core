package com.firmaya.api.contratos.dto;

import com.firmaya.api.contratos.VersionContrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumenVersionContratoDto(
        UUID id,
        int numero,
        String autor,
        OffsetDateTime fechaCreacion,
        String comentario,
        String hashSha256,
        boolean esActual
) {
    public static ResumenVersionContratoDto desde(VersionContrato version, boolean esActual) {
        return new ResumenVersionContratoDto(
                version.getId(),
                version.getNumeroVersion(),
                version.getUsuarioAutor().getNombre() + " " + version.getUsuarioAutor().getApellido(),
                version.getFechaCreacion(),
                version.getComentarioVersion(),
                version.getHashSha256(),
                esActual);
    }
}
