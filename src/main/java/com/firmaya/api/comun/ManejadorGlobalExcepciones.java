package com.firmaya.api.comun;

import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.CredencialesInvalidasException;
import com.firmaya.api.comun.excepciones.CuentaBloqueadaException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.comun.excepciones.TokenInvalidoException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiDto> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.valueOf(422))
                .body(ErrorApiDto.de("VALIDACION", "Los datos enviados no son validos.", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApiDto> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorApiDto.de("FORMATO_INVALIDO", "El cuerpo de la solicitud no es valido."));
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorApiDto> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        Object detalles = ex.getDetalles().isEmpty() ? null : ex.getDetalles();
        return ResponseEntity.status(HttpStatus.valueOf(422))
                .body(ErrorApiDto.de("VALIDACION", ex.getMessage(), detalles));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApiDto> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorApiDto.de("NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoNegocioException.class)
    public ResponseEntity<ErrorApiDto> manejarAccesoDenegadoNegocio(AccesoDenegadoNegocioException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApiDto.de("ACCESO_DENEGADO", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApiDto> manejarAccesoDenegadoSeguridad(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApiDto.de("ACCESO_DENEGADO", "No tiene permisos para esta operacion."));
    }

    @ExceptionHandler(ConflictoEstadoException.class)
    public ResponseEntity<ErrorApiDto> manejarConflicto(ConflictoEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApiDto.de("CONFLICTO", ex.getMessage()));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorApiDto> manejarTokenInvalido(TokenInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApiDto.de("TOKEN_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiradoException.class)
    public ResponseEntity<ErrorApiDto> manejarTokenExpirado(TokenExpiradoException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorApiDto.de("TOKEN_EXPIRADO", ex.getMessage()));
    }

    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorApiDto> manejarCredencialesInvalidas(RuntimeException ex) {
        // El mensaje lo decide cada llamador (CredencialesInvalidasException siempre se
        // lanza con un mensaje explicito): en CU-19 es el texto generico que no distingue
        // correo de contrasena; en CU-08 (OTP) incluye los intentos restantes, tal como
        // exige la especificacion. BadCredentialsException (Spring Security) no trae mensaje
        // propio util, por eso conserva el texto generico como respaldo.
        String mensaje = ex instanceof CredencialesInvalidasException && ex.getMessage() != null
                ? ex.getMessage()
                : "El correo electronico o la contrasena son incorrectos.";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApiDto.de("CREDENCIALES_INVALIDAS", mensaje));
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorApiDto> manejarCuentaBloqueada(CuentaBloqueadaException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        if (ex.getBloqueadaHasta() != null) {
            detalles.put("bloqueadaHasta", ex.getBloqueadaHasta().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ErrorApiDto.de("CUENTA_BLOQUEADA", ex.getMessage(), detalles));
    }
}
