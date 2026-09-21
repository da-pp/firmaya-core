package com.firmaya.api.documentofinal.dto;

import com.firmaya.api.documentofinal.DocumentoFinal;
import com.firmaya.api.documentofinal.EstadoDocumentoFinal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Nunca incluye ubicacionPrivada: es un detalle interno de almacenamiento (ver CU-10, EP-56). */
public record DocumentoFinalDto(
        UUID id,
        UUID idContrato,
        UUID idVersionOrigen,
        String hashContenido,
        String hashPdf,
        String nombreArchivo,
        OffsetDateTime fechaGeneracion,
        EstadoDocumentoFinal estado
) {
    public static DocumentoFinalDto desde(DocumentoFinal documento) {
        return new DocumentoFinalDto(
                documento.getId(),
                documento.getContrato().getId(),
                documento.getIdVersionContrato(),
                documento.getHashContenido(),
                documento.getHashPdf(),
                documento.getNombreArchivo(),
                documento.getFechaGeneracion(),
                documento.getEstado());
    }
}
