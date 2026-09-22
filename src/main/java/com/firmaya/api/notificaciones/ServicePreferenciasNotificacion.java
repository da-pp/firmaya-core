package com.firmaya.api.notificaciones;

import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.notificaciones.dto.PreferenciasNotificacionDto;
import com.firmaya.api.notificaciones.dto.SolicitudActualizarPreferenciasNotificacion;
import com.firmaya.api.notificaciones.dto.TipoEventoNotificacionDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-20, EP-59..EP-61. */
@Service
@Transactional
public class ServicePreferenciasNotificacion {

    private final RepositoryTipoEventoNotificacion repositoryTipoEventoNotificacion;
    private final RepositoryPreferenciaNotificacion repositoryPreferenciaNotificacion;
    private final RepositoryCanalPreferidoUsuario repositoryCanalPreferidoUsuario;

    public ServicePreferenciasNotificacion(RepositoryTipoEventoNotificacion repositoryTipoEventoNotificacion,
                                             RepositoryPreferenciaNotificacion repositoryPreferenciaNotificacion,
                                             RepositoryCanalPreferidoUsuario repositoryCanalPreferidoUsuario) {
        this.repositoryTipoEventoNotificacion = repositoryTipoEventoNotificacion;
        this.repositoryPreferenciaNotificacion = repositoryPreferenciaNotificacion;
        this.repositoryCanalPreferidoUsuario = repositoryCanalPreferidoUsuario;
    }

    @Transactional(readOnly = true)
    public List<TipoEventoNotificacionDto> obtenerTiposEvento() {
        return repositoryTipoEventoNotificacion.findByActivoTrue().stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PreferenciasNotificacionDto obtenerMisPreferencias(UUID idUsuario) {
        List<TipoEventoNotificacion> configurables = tiposConfigurables();
        List<PreferenciaNotificacion> preferenciasExistentes = repositoryPreferenciaNotificacion.findByIdUsuario(idUsuario);
        Map<UUID, PreferenciaNotificacion> porTipo = new LinkedHashMap<>();
        preferenciasExistentes.forEach(preferencia -> porTipo.put(preferencia.getIdTipoEvento(), preferencia));

        Map<String, Boolean> eventosHabilitados = new LinkedHashMap<>();
        OffsetDateTime ultimaActualizacion = null;
        for (TipoEventoNotificacion tipo : configurables) {
            PreferenciaNotificacion preferencia = porTipo.get(tipo.getId());
            eventosHabilitados.put(tipo.getCodigo(), preferencia == null || preferencia.isHabilitada());
        }

        List<CanalNotificacion> canales = repositoryCanalPreferidoUsuario.findByIdUsuario(idUsuario).stream()
                .map(CanalPreferidoUsuario::getCanal)
                .toList();
        // Sin preferencia explicita: ambos canales habilitados por defecto (PD-11 pendiente de definicion).
        if (canales.isEmpty()) {
            canales = List.of(CanalNotificacion.CORREO_ELECTRONICO, CanalNotificacion.PLATAFORMA);
        }

        return new PreferenciasNotificacionDto(eventosHabilitados, canales, ultimaActualizacion);
    }

    public PreferenciasNotificacionDto reemplazarMisPreferencias(UUID idUsuario,
                                                                  SolicitudActualizarPreferenciasNotificacion solicitud) {
        if (solicitud.canales() == null || solicitud.canales().isEmpty()) {
            throw new SolicitudInvalidaException("Debe seleccionar al menos un canal de notificacion.");
        }

        List<TipoEventoNotificacion> configurables = tiposConfigurables();
        Map<String, TipoEventoNotificacion> porCodigo = new LinkedHashMap<>();
        configurables.forEach(tipo -> porCodigo.put(tipo.getCodigo(), tipo));

        Map<String, Boolean> eventosSolicitados = solicitud.eventosHabilitados() == null
                ? Map.of() : solicitud.eventosHabilitados();
        for (String codigo : eventosSolicitados.keySet()) {
            if (!porCodigo.containsKey(codigo)) {
                throw new SolicitudInvalidaException(
                        "El evento '" + codigo + "' no existe o no es configurable.",
                        Map.of("evento", codigo));
            }
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        repositoryCanalPreferidoUsuario.deleteByIdUsuario(idUsuario);
        List<CanalNotificacion> canalesUnicos = solicitud.canales().stream().distinct().toList();
        List<CanalPreferidoUsuario> nuevosCanales = new ArrayList<>();
        for (CanalNotificacion canal : canalesUnicos) {
            nuevosCanales.add(new CanalPreferidoUsuario(idUsuario, canal, ahora));
        }
        repositoryCanalPreferidoUsuario.saveAll(nuevosCanales);

        for (TipoEventoNotificacion tipo : configurables) {
            boolean habilitada = eventosSolicitados.getOrDefault(tipo.getCodigo(), Boolean.TRUE);
            PreferenciaNotificacion existente = repositoryPreferenciaNotificacion
                    .findByIdUsuarioAndIdTipoEvento(idUsuario, tipo.getId()).orElse(null);
            if (existente == null) {
                repositoryPreferenciaNotificacion.save(
                        new PreferenciaNotificacion(idUsuario, tipo.getId(), habilitada, ahora));
            } else {
                existente.actualizar(habilitada, ahora);
            }
        }

        return obtenerMisPreferencias(idUsuario);
    }

    private List<TipoEventoNotificacion> tiposConfigurables() {
        return repositoryTipoEventoNotificacion.findByActivoTrue().stream()
                .filter(TipoEventoNotificacion::isConfigurable)
                .toList();
    }

    private TipoEventoNotificacionDto aDto(TipoEventoNotificacion tipo) {
        List<CanalNotificacion> canales = new ArrayList<>();
        if (tipo.isPermiteCorreo()) {
            canales.add(CanalNotificacion.CORREO_ELECTRONICO);
        }
        if (tipo.isPermitePlataforma()) {
            canales.add(CanalNotificacion.PLATAFORMA);
        }
        return new TipoEventoNotificacionDto(tipo.getCodigo(), tipo.getEtiqueta(), tipo.isConfigurable(), canales);
    }
}
