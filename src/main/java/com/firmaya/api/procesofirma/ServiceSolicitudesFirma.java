package com.firmaya.api.procesofirma;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.contratos.EstadoContrato;
import com.firmaya.api.contratos.RepositoryContrato;
import com.firmaya.api.contratos.ServiceAutorizacionContrato;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.procesofirma.dto.ElementoEntregaDto;
import com.firmaya.api.procesofirma.dto.ElementoEnvioSolicitudDto;
import com.firmaya.api.procesofirma.dto.EnvioSolicitudesFirmaDto;
import com.firmaya.api.procesofirma.dto.ResultadoEntregaLoteDto;
import com.firmaya.api.procesofirma.dto.SolicitudEnviarSolicitudesFirma;
import com.firmaya.api.procesofirma.dto.SolicitudFirmaDto;
import com.firmaya.api.procesofirma.dto.SolicitudReintentarFirmasFallidas;
import com.firmaya.api.procesofirma.dto.SolicitudRenovarSolicitudFirma;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-07/CU-09, EP-47, EP-48, EP-49 y EP-50. */
@Service
@Transactional
public class ServiceSolicitudesFirma {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryProcesoFirma repositoryProcesoFirma;
    private final RepositoryFirmanteProceso repositoryFirmanteProceso;
    private final RepositorySolicitudFirma repositorySolicitudFirma;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final ServiceAutorizacionContrato serviceAutorizacionContrato;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final int diasDefecto;
    private final String urlAcceso;

    public ServiceSolicitudesFirma(RepositoryContrato repositoryContrato,
                                     RepositoryProcesoFirma repositoryProcesoFirma,
                                     RepositoryFirmanteProceso repositoryFirmanteProceso,
                                     RepositorySolicitudFirma repositorySolicitudFirma,
                                     RepositoryTokenAcceso repositoryTokenAcceso,
                                     ServiceAutorizacionContrato serviceAutorizacionContrato,
                                     ServiceHashCredencial serviceHashCredencial,
                                     ServiceEnvioNotificaciones serviceEnvioNotificaciones,
                                     ServiceRegistroAuditoria serviceRegistroAuditoria,
                                     @Value("${firmaya.firma.solicitud-dias-defecto}") int diasDefecto,
                                     @Value("${firmaya.frontend.url-acceso-externo}") String urlAcceso) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryProcesoFirma = repositoryProcesoFirma;
        this.repositoryFirmanteProceso = repositoryFirmanteProceso;
        this.repositorySolicitudFirma = repositorySolicitudFirma;
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.serviceAutorizacionContrato = serviceAutorizacionContrato;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.diasDefecto = diasDefecto;
        this.urlAcceso = urlAcceso;
    }

    public EnvioSolicitudesFirmaDto enviarSolicitudesIniciales(UUID idContrato, UUID idUsuario,
                                                                SolicitudEnviarSolicitudesFirma solicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);
        if (contrato.getEstado() != EstadoContrato.LISTO_PARA_FIRMAR) {
            throw new ConflictoEstadoException("El contrato debe estar Listo para firmar para enviar solicitudes.");
        }
        if (solicitud.fechaLimite() != null && !solicitud.fechaLimite().isAfter(java.time.LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha limite debe ser posterior a la fecha actual.");
        }

        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));
        List<FirmanteProceso> firmantes = repositoryFirmanteProceso.findByProcesoFirmaId(proceso.getId());

        OffsetDateTime ahora = OffsetDateTime.now();
        List<ElementoEnvioSolicitudDto> resultado = new ArrayList<>();
        for (FirmanteProceso firmante : firmantes) {
            SolicitudFirma existente = repositorySolicitudFirma.findByFirmanteProcesoId(firmante.getId()).orElse(null);
            if (existente != null) {
                resultado.add(new ElementoEnvioSolicitudDto(existente.getId(), firmante.getParticipante().getId(),
                        existente.getEstado().name(), existente.getFechaExpiracionEnlace()));
                continue;
            }
            SolicitudFirma nueva = SolicitudFirma.crear(UUID.randomUUID(), proceso, firmante, solicitud.mensaje(),
                    solicitud.fechaLimite(), ahora);
            // Se reasigna save(): con @Id manual, save() usa merge() y la instancia original
            // queda desconectada; enviarNotificacionSolicitud debe mutar la copia gestionada
            // para que marcarNotificada/marcarErrorEntrega se sincronicen con la base.
            nueva = repositorySolicitudFirma.save(nueva);
            enviarNotificacionSolicitud(contrato, firmante, nueva, ahora);
            resultado.add(new ElementoEnvioSolicitudDto(nueva.getId(), firmante.getParticipante().getId(),
                    nueva.getEstado().name(), nueva.getFechaExpiracionEnlace()));
        }

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(contrato.getResponsable(), "SOLICITUDES_FIRMA_ENVIADAS", "ProcesoFirma",
                        "Solicitudes de firma iniciales enviadas para " + firmantes.size() + " firmante(s).")
                .conEntidad(proceso.getId())
                .conContrato(idContrato));

        int completadas = (int) resultado.stream().filter(r -> "FIRMADA".equals(r.estado())).count();
        return new EnvioSolicitudesFirmaDto(proceso.getId(), resultado, completadas, resultado.size());
    }

    public ResultadoEntregaLoteDto reintentarNotificacionesFallidas(UUID idContrato, UUID idUsuario,
                                                                     SolicitudReintentarFirmasFallidas solicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));
        List<SolicitudFirma> candidatas = repositorySolicitudFirma.findByProcesoFirmaId(proceso.getId()).stream()
                .filter(s -> s.getEstadoEntrega() == EstadoEntregaSolicitud.ERROR)
                .filter(s -> solicitud.idsSolicitudes() == null || solicitud.idsSolicitudes().isEmpty()
                        || solicitud.idsSolicitudes().contains(s.getId()))
                .toList();

        OffsetDateTime ahora = OffsetDateTime.now();
        List<ElementoEntregaDto> detalles = new ArrayList<>();
        int entregados = 0;
        for (SolicitudFirma solicitudFirma : candidatas) {
            FirmanteProceso firmante = solicitudFirma.getFirmanteProceso();
            boolean enviado = enviarNotificacionSolicitud(contrato, firmante, solicitudFirma, ahora);
            detalles.add(new ElementoEntregaDto(solicitudFirma.getId(), enviado));
            if (enviado) {
                entregados++;
            }
        }

        return new ResultadoEntregaLoteDto(candidatas.size(), entregados, candidatas.size() - entregados, detalles);
    }

    /**
     * CU-09, EP-49: reenvia la misma solicitud a un firmante pendiente NO vencido. Rota el
     * enlace (revoca el token vigente y emite uno nuevo) para no reutilizar una credencial ya
     * distribuida; conserva version/hash objetivo y el historial de la solicitud.
     */
    public SolicitudFirmaDto reenviar(UUID idContrato, UUID idUsuario, UUID idSolicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));
        SolicitudFirma solicitud = repositorySolicitudFirma.findByIdAndProcesoFirmaId(idSolicitud, proceso.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud de firma no existe."));
        if (solicitud.getEstado() == EstadoSolicitudFirma.FIRMADA) {
            throw new ConflictoEstadoException("El firmante ya completo su firma; no corresponde reenviar.");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!solicitud.estaVigente(ahora)) {
            throw new TokenExpiradoException("La solicitud vencio; utilice la renovacion en su lugar.");
        }

        revocarTokensFirmaVigentes(idSolicitud, ahora, "Reemplazado por un reenvio de la solicitud.");
        enviarNotificacionSolicitud(contrato, solicitud.getFirmanteProceso(), solicitud, ahora);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(contrato.getResponsable(), "SOLICITUD_FIRMA_REENVIADA", "SolicitudFirma",
                        "Solicitud de firma reenviada al firmante.")
                .conEntidad(solicitud.getId())
                .conContrato(idContrato));

        return aSolicitudFirmaDto(solicitud);
    }

    /**
     * CU-09, EP-50: renueva una solicitud vencida sin afectar firmas ya registradas de otros
     * firmantes ni la version/hash objetivo del proceso. Revoca el enlace anterior y emite uno
     * nuevo; opcionalmente actualiza la fecha limite individual de este firmante.
     */
    public SolicitudFirmaDto renovar(UUID idContrato, UUID idUsuario, UUID idSolicitud,
                                      SolicitudRenovarSolicitudFirma solicitud) {
        Contrato contrato = repositoryContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        serviceAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        ProcesoFirma proceso = repositoryProcesoFirma.findByContratoIdAndFinalizadoFalse(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un proceso de firma activo."));
        SolicitudFirma solicitudFirma = repositorySolicitudFirma.findByIdAndProcesoFirmaId(idSolicitud, proceso.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud de firma no existe."));
        if (solicitudFirma.getEstado() == EstadoSolicitudFirma.FIRMADA) {
            throw new ConflictoEstadoException("El firmante ya completo su firma; no corresponde renovar.");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (solicitudFirma.estaVigente(ahora)) {
            throw new ConflictoEstadoException("La solicitud aun esta vigente; utilice el reenvio en su lugar.");
        }
        LocalDate nuevaFechaLimite = solicitud.fechaLimiteNueva();
        if (nuevaFechaLimite != null && !nuevaFechaLimite.isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("La nueva fecha limite debe ser posterior a la fecha actual.");
        }

        revocarTokensFirmaVigentes(idSolicitud, ahora, "Reemplazado por una renovacion de la solicitud.");

        FirmanteProceso firmante = solicitudFirma.getFirmanteProceso();
        LocalDate fechaLimiteEfectiva = nuevaFechaLimite != null ? nuevaFechaLimite : solicitudFirma.getFechaLimite();
        OffsetDateTime expiracionToken = calcularExpiracion(fechaLimiteEfectiva, ahora);

        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        TokenAcceso token = TokenAcceso.deFirma(UUID.randomUUID(), idSolicitud, hash, ahora, expiracionToken);
        repositoryTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Se renovo tu solicitud de firma para el contrato \"" + contrato.getNombre()
                + "\" en FirmaYA. " + (solicitud.mensaje() != null ? solicitud.mensaje() + " " : "")
                + "Accede con el siguiente enlace para revisar y firmar: " + enlace;
        String resumen = "Se renovo una solicitud de firma vencida.";
        boolean enviado = serviceEnvioNotificaciones.enviarCorreoDirigidoAParticipante(contrato.getId(),
                firmante.getParticipante().getId(), firmante.getCorreoCongelado(), "CONTRATO_SOLICITUD_FIRMA",
                "Solicitud de firma renovada - FirmaYA", cuerpoCorreo, resumen);
        if (enviado) {
            solicitudFirma.renovar(nuevaFechaLimite, ahora, expiracionToken);
        } else {
            solicitudFirma.marcarErrorEntrega(ahora);
        }

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(contrato.getResponsable(), "SOLICITUD_FIRMA_RENOVADA", "SolicitudFirma",
                        "Solicitud de firma renovada tras vencimiento.")
                .conEntidad(solicitudFirma.getId())
                .conContrato(idContrato));

        return aSolicitudFirmaDto(solicitudFirma);
    }

    private void revocarTokensFirmaVigentes(UUID idSolicitud, OffsetDateTime ahora, String motivo) {
        repositoryTokenAcceso.findByIdSolicitudFirmaAndFechaConsumoIsNullAndFechaRevocacionIsNull(idSolicitud)
                .forEach(token -> token.revocar(ahora, motivo));
    }

    private SolicitudFirmaDto aSolicitudFirmaDto(SolicitudFirma solicitud) {
        return new SolicitudFirmaDto(solicitud.getId(), solicitud.getFirmanteProceso().getParticipante().getId(),
                solicitud.getEstado().name(), solicitud.getFechaUltimoEnvio(), solicitud.getFechaLimite(),
                solicitud.getFechaExpiracionEnlace(), solicitud.getEstadoEntrega().name());
    }

    private boolean enviarNotificacionSolicitud(Contrato contrato, FirmanteProceso firmante, SolicitudFirma solicitudFirma,
                                                 OffsetDateTime ahora) {
        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracionToken = calcularExpiracion(solicitudFirma, ahora);
        TokenAcceso token = TokenAcceso.deFirma(UUID.randomUUID(), solicitudFirma.getId(), hash, ahora, expiracionToken);
        repositoryTokenAcceso.save(token);

        String enlace = urlAcceso + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Se solicita tu firma para el contrato \"" + contrato.getNombre() + "\" en FirmaYA. "
                + "Accede con el siguiente enlace para revisar y firmar: " + enlace;
        String resumen = "Se envio una solicitud de firma.";

        boolean enviado = serviceEnvioNotificaciones.enviarCorreoDirigidoAParticipante(contrato.getId(),
                firmante.getParticipante().getId(), firmante.getCorreoCongelado(), "CONTRATO_SOLICITUD_FIRMA",
                "Solicitud de firma - FirmaYA", cuerpoCorreo, resumen);
        if (enviado) {
            solicitudFirma.marcarNotificada(ahora, expiracionToken);
        } else {
            solicitudFirma.marcarErrorEntrega(ahora);
        }
        return enviado;
    }

    private OffsetDateTime calcularExpiracion(SolicitudFirma solicitudFirma, OffsetDateTime ahora) {
        return calcularExpiracion(solicitudFirma.getFechaLimite(), ahora);
    }

    private OffsetDateTime calcularExpiracion(LocalDate fechaLimite, OffsetDateTime ahora) {
        if (fechaLimite != null) {
            return fechaLimite.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
        }
        return ahora.plusDays(diasDefecto);
    }
}
