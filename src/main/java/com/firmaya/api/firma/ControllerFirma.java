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
public class ControllerFirma {

    private final ServiceConsultaFirmante serviceConsultaFirmante;
    private final ServiceAceptacionFirma serviceAceptacionFirma;
    private final ServiceOtpFirma serviceOtpFirma;
    private final ServiceFinalizacionFirma serviceFinalizacionFirma;

    public ControllerFirma(ServiceConsultaFirmante serviceConsultaFirmante,
                             ServiceAceptacionFirma serviceAceptacionFirma,
                             ServiceOtpFirma serviceOtpFirma,
                             ServiceFinalizacionFirma serviceFinalizacionFirma) {
        this.serviceConsultaFirmante = serviceConsultaFirmante;
        this.serviceAceptacionFirma = serviceAceptacionFirma;
        this.serviceOtpFirma = serviceOtpFirma;
        this.serviceFinalizacionFirma = serviceFinalizacionFirma;
    }

    @GetMapping("/contexto")
    public ContextoFirmaDto obtenerContexto(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto) {
        return serviceConsultaFirmante.obtenerContexto(contexto);
    }

    @PostMapping("/aceptacion")
    public ResponseEntity<AceptacionFirmaDto> registrarAceptacion(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                                    @Valid @RequestBody SolicitudAceptacionFirma solicitud,
                                                                    HttpServletRequest request) {
        AceptacionFirmaDto aceptacion = serviceAceptacionFirma.registrarAceptacion(contexto, solicitud,
                request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(aceptacion);
    }

    @PostMapping("/otp")
    public ResponseEntity<DesafioOtpDto> emitirOtp(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                     @Valid @RequestBody SolicitudEmitirOtp solicitud) {
        DesafioOtpDto desafio = serviceOtpFirma.emitir(contexto, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(desafio);
    }

    @PostMapping("/otp/reenviar")
    public ResponseEntity<DesafioOtpDto> reenviarOtp(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                       @Valid @RequestBody SolicitudReenviarOtp solicitud) {
        DesafioOtpDto desafio = serviceOtpFirma.reenviar(contexto, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(desafio);
    }

    @PostMapping("/completar")
    public ResponseEntity<ResultadoFirmaDto> completar(@AuthenticationPrincipal ContextoParticipanteAutenticado contexto,
                                                         @Valid @RequestBody SolicitudCompletarFirma solicitud,
                                                         HttpServletRequest request) {
        ResultadoFirmaDto resultado = serviceFinalizacionFirma.completar(contexto, solicitud, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
