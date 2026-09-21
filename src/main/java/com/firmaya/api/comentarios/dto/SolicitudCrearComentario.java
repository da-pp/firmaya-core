package com.firmaya.api.comentarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCrearComentario(
        @NotBlank(message = "El comentario no puede estar vacio")
        @Size(max = 1000, message = "El comentario no puede superar 1000 caracteres")
        String texto,

        String textoSeleccionado
) {
}
