package com.firmaya.api.firma;

import com.firmaya.api.accesos.ContextoParticipanteAutenticado;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.CredencialesInvalidasException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.HistorialEstadoContrato;
import com.firmaya.api.contratos.RepositoryHistorialEstadoContrato;
import com.firmaya.api.firma.dto.ProgresoFirmaDto;
import com.firmaya.api.firma.dto.ResultadoFirmaDto;
import com.firmaya.api.firma.dto.SolicitudCompletarFirma;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.RepositoryFirmanteProceso;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08, EP-55: verifica el OTP y persiste la firma de forma atomica; si con esta firma se
 * completan todos los firmantes requeridos, el contrato pasa automaticamente a Firmado.
 */
@Service
@Transactional
public class ServiceFinalizacionFirma {

    private final ServiceResolucionFirmante serviceResolucionFirmante;
    private final RepositoryAceptacionFirma repositoryAceptacionFirma;
    private final RepositoryDesafioOtp repositoryDesafioOtp;
    private final RepositoryFirma repositoryFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato;
    private final ServiceVerificadorOtp serviceVerificadorOtp;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceFinalizacionFirma(ServiceResolucionFirmante serviceResolucionFirmante,
                                      RepositoryAceptacionFirma repositoryAceptacionFirma,
                                      RepositoryDesafioOtp repositoryDesafioOtp,
                                      RepositoryFirma repositoryFirma,
                                      RepositoryFirmanteProceso repositoryFirmanteProceso,
                                      RepositoryHistorialEstadoContrato repositoryHistorialEstadoContrato,
                                      ServiceVerificadorOtp serviceVerificadorOtp,
                                      ServiceEnvioNotificaciones serviceEnvioNotificaciones,
                                      ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.serviceResolucionFirmante = serviceResolucionFirmante;
        this.repositoryAceptacionFirma = repositoryAceptacionFirma;
        this.repositoryDesafioOtp = repositoryDesafioOtp;
        this.repositoryFirma = repositoryFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositoryHistorialEstadoContrato = repositoryHistorialEstadoContrato;
        this.serviceVerificadorOtp = serviceVerificadorOtp;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    public ResultadoFirmaDto completar(ContextoParticipanteAutenticado contexto, SolicitudCompletarFirma solicitud,
                                        String direccionIp) {
        ResolucionFirmante resolucion = serviceResolucionFirmante.resolver(contexto);
        if (!resolucion.solicitud().getId().equals(solicitud.idSolicitud())) {
            throw new SolicitudInvalidaException("La solicitud indicada no corresponde a su acceso.");
        }

        // Idempotencia: un reintento sobre una firma ya registrada no debe duplicarla ni fallar.
        var firmaExistente = repositoryFirma.findByFirmanteProcesoId(resolucion.firmante().getId());
        if (firmaExistente.isPresent()) {
            return construirResultado(resolucion.contrato(), firmaExistente.get(), resolucion.proceso().getId());
        }

        if (!resolucion.solicitud().estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de firma ha vencido.");
        }

        DesafioOtp desafio = repositoryDesafioOtp.findByIdAndIdSolicitudFirma(solicitud.idDesafio(), solicitud.idSolicitud())
                .orElseThrow(() -> new RecursoNoEncontradoException("El desafio OTP no existe."));
        if (desafio.estaBloqueado()) {
            throw new ConflictoEstadoException("El enlace de firma ha sido bloqueado por seguridad. Contacte al dueno del contrato.");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!desafio.estaVigente(ahora)) {
            throw new TokenExpiradoException("El codigo ha expirado. Solicite uno nuevo.");
        }

        if (!serviceVerificadorOtp.coincide(solicitud.otp(), solicitud.idSolicitud(), desafio.getVerificadorOtp())) {
            desafio.registrarIntentoFallido(ahora);
            int restantes = 3 - desafio.getIntentosFallidos();
            if (restantes <= 0) {
                throw new ConflictoEstadoException(
                        "El enlace de firma ha sido bloqueado por seguridad. Contacte al dueno del contrato.");
            }
            throw new CredencialesInvalidasException(
                    "El codigo ingresado es incorrecto. Intentos restantes: " + restantes + ".");
        }
        desafio.consumir(ahora);

        AceptacionFirma aceptacion = repositoryAceptacionFirma.findBySolicitudFirmaId(solicitud.idSolicitud())
                .orElseThrow(() -> new ConflictoEstadoException("No existe una aceptacion registrada para esta solicitud."));

        Firma firma = Firma.crear(UUID.randomUUID(), resolucion.solicitud(), resolucion.proceso(),
                resolucion.firmante(), aceptacion, desafio, resolucion.proceso().getIdVersionObjetivo(),
                resolucion.proceso().getHashObjetivo(), direccionIp, ahora);
        repositoryFirma.save(firma);
        resolucion.solicitud().marcarFirmada(ahora);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("FIRMA_REGISTRADA", "Firma", "Firma registrada exitosamente tras verificacion de OTP.")
                .conEntidad(firma.getId())
                .conVersionContrato(resolucion.contrato().getId(), resolucion.proceso().getIdVersionObjetivo())
                .conIp(direccionIp));

        List<FirmanteProceso> todos = repositoryFirmanteProceso.findByProcesoFirmaId(resolucion.proceso().getId());
        long firmados = todos.stream().filter(f -> repositoryFirma.findByFirmanteProcesoId(f.getId()).isPresent()).count();

        Contrato contrato = resolucion.contrato();
        if (firmados == todos.size()) {
            EstadoContrato anterior = contrato.getEstado();
            resolucion.proceso().completar(ahora);
            contrato.cambiarEstado(EstadoContrato.FIRMADO, ahora);
            repositoryHistorialEstadoContrato.save(HistorialEstadoContrato.crear(UUID.randomUUID(), contrato, anterior,
                    EstadoContrato.FIRMADO, null, resolucion.proceso().getId(),
                    "Todos los firmantes requeridos completaron su firma.", ahora));

            serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                    .deSistema("CONTRATO_FIRMADO", "Contrato", "Todos los firmantes completaron su firma; contrato Firmado.")
                    .conContrato(contrato.getId()));

            serviceEnvioNotificaciones.enviarCorreoOperativo(contrato.getResponsable(), "CONTRATO_FIRMADO",
                    "Contrato firmado - FirmaYA",
                    "El contrato \"" + contrato.getNombre() + "\" fue firmado por todas las partes requeridas.");
        }

        return construirResultado(contrato, firma, resolucion.proceso().getId());
    }

    private ResultadoFirmaDto construirResultado(Contrato contrato, Firma firma, UUID idProceso) {
        List<FirmanteProceso> todos = repositoryFirmanteProceso.findByProcesoFirmaId(idProceso);
        long completadas = todos.stream().filter(f -> repositoryFirma.findByFirmanteProcesoId(f.getId()).isPresent()).count();
        return new ResultadoFirmaDto(firma.getId(), firma.getSolicitudFirma().getId(), contrato.getId(),
                firma.getIdVersionContrato(), firma.getHashVersion(),
                firma.getFechaFirma(), "FIRMADO", contrato.getEstado(),
                new ProgresoFirmaDto((int) completadas, todos.size()));
    }
}
