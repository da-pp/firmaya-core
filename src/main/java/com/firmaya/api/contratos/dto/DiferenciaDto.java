package com.firmaya.api.contratos.dto;

/**
 * Una linea del resultado de comparacion (CU-12). La granularidad exacta del algoritmo de
 * diferencias es PD-07 [PENDIENTE] en la especificacion; esta implementacion compara por
 * linea (separador `\n`), decision tecnica provisional documentada en ServicioComparacionContratos.
 */
public record DiferenciaDto(TipoDiferencia tipo, String texto) {

    public enum TipoDiferencia {
        AGREGADO,
        ELIMINADO,
        SIN_CAMBIO
    }
}
