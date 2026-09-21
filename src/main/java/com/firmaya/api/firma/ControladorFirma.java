package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.firma.dto.AceptacionFirmaDto;
import com.firmaya.api.firma.dto.ContextoFirmaDto;
import com.firmaya.api.firma.dto.DesafioOtpDto;
import com.firmaya.api.firma.dto.ResultadoFirmaDto;
import com.firmaya.api.firma.dto.SolicitudAceptacionFirma;
import com.firmaya.api.firma.dto.SolicitudCompletarFirma;
import com.firmaya.api.firma.dto.SolicitudEmitirOtp;
import com.firmaya.api.firma.dto.SolicitudReenviarOtp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-08, EP-51..EP-55. Requiere sesion externa de proposito FIRMA ligada a la solicitud. */
@RestController
@RequestMapping("/api/v1/firma")
public class ControladorFirma {

    private final ServicioConsultaFirmante servicioConsultaFirmante;
    private final ServicioAceptacionFirma servicioAceptacionFirma;
    private final ServicioOtpFirma servicioOtpFirma;
    private final ServicioFinalizacionFirma servicioFinalizacionFirma;

    public ControladorFirma(ServicioConsultaFirmante servicioConsultaFirmante,
                             ServicioAceptacionFirma servicioAceptacionFirma,
                             ServicioOtpFirma servicioOtpFirma,
                             ServicioFinalizacionFirma servicioFinalizacionFirma) {
        this.servicioConsultaFirmante = servicioConsultaFirmante;
        this.servicioAceptacionFirma = servicioAceptacionFirma;
        this.servicioOtpFirma = servicioOtpFirma;
        this.servicioFinalizacionFirma = servicioFinalizacionFirma;
    }

    @GetMapping("/contexto")
    public ContextoFirmaDto obtenerContexto(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto) {
        return servicioConsultaFirmante.obtenerContexto(contexto);
    }

    @PostMapping("/aceptacion")
    public ResponseEntity<AceptacionFirmaDto> registrarAceptacion(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                                    @Valid @RequestBody SolicitudAceptacionFirma solicitud,
                                                                    HttpServletRequest request) {
        AceptacionFirmaDto aceptacion = servicioAceptacionFirma.registrarAceptacion(contexto, solicitud,
                request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(aceptacion);
    }

    @PostMapping("/otp")
    public ResponseEntity<DesafioOtpDto> emitirOtp(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                     @Valid @RequestBody SolicitudEmitirOtp solicitud) {
        DesafioOtpDto desafio = servicioOtpFirma.emitir(contexto, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(desafio);
    }

    @PostMapping("/otp/reenviar")
    public ResponseEntity<DesafioOtpDto> reenviarOtp(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                       @Valid @RequestBody SolicitudReenviarOtp solicitud) {
        DesafioOtpDto desafio = servicioOtpFirma.reenviar(contexto, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(desafio);
    }

    @PostMapping("/completar")
    public ResponseEntity<ResultadoFirmaDto> completar(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                         @Valid @RequestBody SolicitudCompletarFirma solicitud,
                                                         HttpServletRequest request) {
        ResultadoFirmaDto resultado = servicioFinalizacionFirma.completar(contexto, solicitud, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
