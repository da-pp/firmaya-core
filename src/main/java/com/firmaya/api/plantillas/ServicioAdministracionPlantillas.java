package com.firmaya.api.plantillas;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.dto.DefinicionCampoDinamicoDto;
import com.firmaya.api.plantillas.dto.PlantillaAdministracionDto;
import com.firmaya.api.plantillas.dto.ResumenVersionPlantillaDto;
import com.firmaya.api.plantillas.dto.SolicitudCambiarEstadoPlantilla;
import com.firmaya.api.plantillas.dto.SolicitudCrearPlantilla;
import com.firmaya.api.plantillas.dto.SolicitudCrearVersionPlantilla;
import com.firmaya.api.plantillas.dto.VersionPlantillaDto;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-16, EP-21..EP-27: administracion de plantillas versionadas con marcadores dinamicos.
 * Editar una plantilla nunca modifica los contratos ya creados desde una version anterior
 * (Contrato guarda idPlantilla/idVersionPlantilla propios); esta clase solo agrega versiones
 * nuevas, nunca reescribe una VersionPlantilla existente (protegida por trigger en la base).
 */
@Service
@Transactional
public class ServicioAdministracionPlantillas {

    private static final int TAMANO_PAGINA = 20;
    private static final Pattern PATRON_MARCADOR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    private final RepositorioPlantilla repositorioPlantilla;
    private final RepositorioVersionPlantilla repositorioVersionPlantilla;
    private final RepositorioCampoPlantilla repositorioCampoPlantilla;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;

    public ServicioAdministracionPlantillas(RepositorioPlantilla repositorioPlantilla,
                                             RepositorioVersionPlantilla repositorioVersionPlantilla,
                                             RepositorioCampoPlantilla repositorioCampoPlantilla,
                                             RepositorioUsuario repositorioUsuario,
                                             ServicioRegistroAuditoria servicioRegistroAuditoria) {
        this.repositorioPlantilla = repositorioPlantilla;
        this.repositorioVersionPlantilla = repositorioVersionPlantilla;
        this.repositorioCampoPlantilla = repositorioCampoPlantilla;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
    }

    @Transactional(readOnly = true)
    public PaginaDto<PlantillaAdministracionDto> buscarPlantillas(TipoContrato tipo, EstadoPlantilla estado, Integer pagina) {
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA, Sort.by(Sort.Direction.ASC, "nombre"));
        var resultado = repositorioPlantilla.buscarPlantillas(tipo, estado, paginado);
        return PaginaDto.desde(resultado.map(this::aDto));
    }

    @Transactional(readOnly = true)
    public PlantillaAdministracionDto obtenerPlantilla(UUID idPlantilla) {
        return aDto(obtenerPlantillaOLanzar(idPlantilla));
    }

    public PlantillaAdministracionDto crearPlantilla(UUID idUsuario, SolicitudCrearPlantilla solicitud) {
        Usuario autor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        List<DefinicionCampoDinamicoDto> campos = validarMarcadores(solicitud.contenido(), solicitud.campos(),
                solicitud.permitirSinCamposDinamicos());

        OffsetDateTime ahora = OffsetDateTime.now();
        Plantilla plantilla = Plantilla.crear(UUID.randomUUID(), solicitud.nombre(), solicitud.tipo(),
                solicitud.descripcion(), solicitud.estado(), autor, ahora);
        plantilla = repositorioPlantilla.save(plantilla);

        VersionPlantilla version = crearVersionConCampos(plantilla, 1, solicitud.contenido(), campos, autor, ahora);

        plantilla.establecerVersionActual(version.getId(), ahora);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(autor, "PLANTILLA_CREADA", "Plantilla", "Plantilla creada con version 1.")
                .conEntidad(plantilla.getId()));

        return aDto(plantilla);
    }

    @Transactional(readOnly = true)
    public PaginaDto<ResumenVersionPlantillaDto> listarVersiones(UUID idPlantilla, Integer pagina) {
        obtenerPlantillaOLanzar(idPlantilla);
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA);
        var resultado = repositorioVersionPlantilla.findByPlantillaIdOrderByNumeroVersionDesc(idPlantilla, paginado);
        return PaginaDto.desde(resultado.map(version -> new ResumenVersionPlantillaDto(
                version.getId(), version.getNumeroVersion(), version.getFechaCreacion(), version.getUsuarioAutor().getId())));
    }

    @Transactional(readOnly = true)
    public VersionPlantillaDto obtenerVersion(UUID idPlantilla, UUID idVersion) {
        VersionPlantilla version = repositorioVersionPlantilla.findByIdAndPlantillaId(idVersion, idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version de plantilla no existe."));
        return aVersionDto(version);
    }

    public VersionPlantillaDto crearVersion(UUID idPlantilla, UUID idUsuario, SolicitudCrearVersionPlantilla solicitud) {
        Plantilla plantilla = obtenerPlantillaOLanzar(idPlantilla);
        if (plantilla.getIdVersionActual() == null || !plantilla.getIdVersionActual().equals(solicitud.idVersionBase())) {
            throw new ConflictoEstadoException(
                    "La version base ya no es la version actual de la plantilla; recargue antes de guardar.");
        }
        Usuario autor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        List<DefinicionCampoDinamicoDto> campos = validarMarcadores(solicitud.contenido(), solicitud.campos(),
                solicitud.permitirSinCamposDinamicos());

        VersionPlantilla versionAnterior = repositorioVersionPlantilla
                .findTopByPlantillaIdOrderByNumeroVersionDesc(idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La plantilla no tiene versiones."));

        OffsetDateTime ahora = OffsetDateTime.now();
        VersionPlantilla nuevaVersion = crearVersionConCampos(plantilla, versionAnterior.getNumeroVersion() + 1,
                solicitud.contenido(), campos, autor, ahora);

        plantilla.establecerVersionActual(nuevaVersion.getId(), ahora);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(autor, "PLANTILLA_VERSION_CREADA", "VersionPlantilla",
                        "Nueva version " + nuevaVersion.getNumeroVersion() + " de la plantilla. "
                                + "Los contratos existentes conservan su version de origen.")
                .conEntidad(nuevaVersion.getId()));

        return aVersionDto(nuevaVersion);
    }

    public PlantillaAdministracionDto cambiarEstado(UUID idPlantilla, SolicitudCambiarEstadoPlantilla solicitud) {
        Plantilla plantilla = obtenerPlantillaOLanzar(idPlantilla);
        if (plantilla.getEstado() == solicitud.estado()) {
            throw new ConflictoEstadoException("La plantilla ya se encuentra en el estado solicitado.");
        }
        plantilla.cambiarEstado(solicitud.estado(), OffsetDateTime.now());

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("PLANTILLA_CAMBIO_ESTADO", "Plantilla", "Estado cambiado a " + solicitud.estado() + ".")
                .conEntidad(plantilla.getId()));

        return aDto(plantilla);
    }

    private VersionPlantilla crearVersionConCampos(Plantilla plantilla, int numeroVersion, String contenido,
                                                     List<DefinicionCampoDinamicoDto> campos, Usuario autor,
                                                     OffsetDateTime ahora) {
        VersionPlantilla version = VersionPlantilla.crear(UUID.randomUUID(), plantilla, numeroVersion, contenido,
                autor, ahora);
        version = repositorioVersionPlantilla.save(version);

        int orden = 0;
        for (DefinicionCampoDinamicoDto campoDto : campos) {
            CampoPlantilla campo = CampoPlantilla.crear(UUID.randomUUID(), version, campoDto.nombreMarcador(),
                    campoDto.etiqueta(), campoDto.tipo(), campoDto.obligatorio(), orden++, campoDto.valorPredeterminado());
            repositorioCampoPlantilla.save(campo);
        }
        return version;
    }

    /**
     * CU-16, Alternativa 1 y 3: si el contenido no tiene marcadores, exige confirmacion
     * explicita (permitirSinCamposDinamicos=true); si tiene marcadores, cada uno debe tener
     * una definicion de campo y cada campo definido debe corresponder a un marcador presente
     * en el contenido (coherencia marcador <-> metadatos).
     */
    private List<DefinicionCampoDinamicoDto> validarMarcadores(String contenido, List<DefinicionCampoDinamicoDto> campos,
                                                                 Boolean permitirSinCamposDinamicos) {
        List<DefinicionCampoDinamicoDto> definiciones = campos != null ? campos : List.of();

        java.util.Set<String> marcadoresEnContenido = new java.util.LinkedHashSet<>();
        Matcher matcher = PATRON_MARCADOR.matcher(contenido);
        while (matcher.find()) {
            marcadoresEnContenido.add(matcher.group(1));
        }

        if (marcadoresEnContenido.isEmpty()) {
            if (!Boolean.TRUE.equals(permitirSinCamposDinamicos)) {
                throw new SolicitudInvalidaException(
                        "La plantilla no tiene campos dinamicos definidos. Confirme permitirSinCamposDinamicos "
                                + "para guardarla de todos modos.");
            }
            return List.of();
        }

        java.util.Set<String> marcadoresDefinidos = definiciones.stream()
                .map(DefinicionCampoDinamicoDto::nombreMarcador).collect(java.util.stream.Collectors.toSet());

        for (String marcador : marcadoresEnContenido) {
            if (!marcadoresDefinidos.contains(marcador)) {
                throw new SolicitudInvalidaException(
                        "El marcador {{" + marcador + "}} no tiene definidos tipo y obligatoriedad.");
            }
        }
        for (DefinicionCampoDinamicoDto campo : definiciones) {
            if (!marcadoresEnContenido.contains(campo.nombreMarcador())) {
                throw new SolicitudInvalidaException(
                        "El campo definido '" + campo.nombreMarcador() + "' no aparece como marcador en el contenido.");
            }
        }
        return definiciones;
    }

    private VersionPlantillaDto aVersionDto(VersionPlantilla version) {
        List<com.firmaya.api.plantillas.dto.CampoDinamicoDto> campos = repositorioCampoPlantilla
                .findByVersionPlantillaIdOrderByOrdenVisualAsc(version.getId()).stream()
                .map(campo -> new com.firmaya.api.plantillas.dto.CampoDinamicoDto(
                        campo.getNombreMarcador(), campo.getEtiqueta(), campo.getTipoDato(),
                        campo.isObligatorio(), campo.getValorPredeterminado()))
                .toList();
        return new VersionPlantillaDto(version.getId(), version.getPlantilla().getId(), version.getNumeroVersion(),
                version.getContenido(), campos, version.getFechaCreacion(), version.getUsuarioAutor().getId());
    }

    private PlantillaAdministracionDto aDto(Plantilla plantilla) {
        int numeroVersion = plantilla.getIdVersionActual() == null ? 0
                : repositorioVersionPlantilla.findById(plantilla.getIdVersionActual())
                        .map(VersionPlantilla::getNumeroVersion).orElse(0);
        return new PlantillaAdministracionDto(plantilla.getId(), plantilla.getNombre(), plantilla.getTipoContrato(),
                plantilla.getDescripcion(), plantilla.getEstado(), plantilla.getIdVersionActual(), numeroVersion);
    }

    private Plantilla obtenerPlantillaOLanzar(UUID idPlantilla) {
        return repositorioPlantilla.findById(idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La plantilla no existe."));
    }
}
