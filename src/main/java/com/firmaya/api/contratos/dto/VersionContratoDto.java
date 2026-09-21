package com.firmaya.api.contratos.dto;

import com.firmaya.api.contratos.VersionContrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VersionContratoDto(
        UUID id,
        UUID idContrato,
        int numeroVersion,
        String contenido,
        String hashSha256,
        UUID idAutor,
        OffsetDateTime fechaCreacion,
        String comentarioCambio,
        UUID idVersionRestaurada
) {
    public static VersionContratoDto desde(VersionContrato version) {
        return new VersionContratoDto(
                version.getId(),
                version.getContrato().getId(),
                version.getNumeroVersion(),
                version.getContenido(),
                version.getHashSha256(),
                version.getUsuarioAutor().getId(),
                version.getFechaCreacion(),
                version.getComentarioVersion(),
                version.getIdVersionRestaurada());
    }
}
