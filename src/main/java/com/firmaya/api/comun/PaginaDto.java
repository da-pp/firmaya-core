package com.firmaya.api.comun;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaDto<T>(
        List<T> elementos,
        int pagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas
) {
    public static <T> PaginaDto<T> desde(Page<T> pagina) {
        return new PaginaDto<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages()
        );
    }
}
