package com.firmaya.api.documentofinal;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.documentofinal.dto.ArchivoDescargaDto;
import com.firmaya.api.documentofinal.dto.DocumentoFinalDto;
import com.firmaya.api.seguridad.ContextoUsuarioAutenticado;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-10, EP-56..EP-58. Accesible por sesion interna (con acceso al contrato) o externa de proposito CONSULTA. */
@RestController
@RequestMapping("/api/v1/contratos/{idContrato}/documento-final")
public class ControladorDocumentoFinal {

    private final ServicioDocumentoFinal servicioDocumentoFinal;

    public ControladorDocumentoFinal(ServicioDocumentoFinal servicioDocumentoFinal) {
        this.servicioDocumentoFinal = servicioDocumentoFinal;
    }

    @GetMapping
    public DocumentoFinalDto obtenerMetadatos(@PathVariable UUID idContrato, Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            return servicioDocumentoFinal.obtenerMetadatosParaUsuarioInterno(idContrato, interno.idUsuario());
        }
        if (principal instanceof ContextoParticipanteAutenticado externo) {
            return servicioDocumentoFinal.obtenerMetadatosParaParticipanteExterno(idContrato, externo);
        }
        throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
    }

    @PostMapping("/generar")
    public ResponseEntity<DocumentoFinalDto> generar(@PathVariable UUID idContrato,
                                                       @AuthenticationPrincipal ContextoUsuarioAutenticado usuario) {
        var resultado = servicioDocumentoFinal.generarIdempotentemente(idContrato, usuario.idUsuario());
        HttpStatus estado = resultado.recienGenerado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.documento());
    }

    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID idContrato, Authentication autenticacion) {
        Object principal = autenticacion.getPrincipal();
        ArchivoDescargaDto archivo;
        if (principal instanceof ContextoUsuarioAutenticado interno) {
            archivo = servicioDocumentoFinal.descargarParaUsuarioInterno(idContrato, interno.idUsuario());
        } else if (principal instanceof ContextoParticipanteAutenticado externo) {
            archivo = servicioDocumentoFinal.descargarParaParticipanteExterno(idContrato, externo);
        } else {
            throw new AccesoDenegadoNegocioException("Sesion no reconocida.");
        }
        String nombreCodificado = URLEncoder.encode(archivo.nombreArchivo(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombreArchivo()
                        + "\"; filename*=UTF-8''" + nombreCodificado)
                .body(archivo.contenido());
    }
}
