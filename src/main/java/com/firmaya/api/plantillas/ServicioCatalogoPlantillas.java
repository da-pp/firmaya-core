package com.firmaya.api.plantillas;

import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.dto.CampoDinamicoDto;
import com.firmaya.api.plantillas.dto.CatalogoPlantillaDto;
import com.firmaya.api.plantillas.dto.FormularioPlantillaDto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-01/CU-16, EP-19 y EP-20. Solo lectura: la administracion de plantillas (CU-16) esta fuera de alcance. */
@Service
@Transactional(readOnly = true)
public class ServicioCatalogoPlantillas {

    private final RepositorioPlantilla repositorioPlantilla;
    private final RepositorioVersionPlantilla repositorioVersionPlantilla;
    private final RepositorioCampoPlantilla repositorioCampoPlantilla;

    public ServicioCatalogoPlantillas(RepositorioPlantilla repositorioPlantilla,
                                       RepositorioVersionPlantilla repositorioVersionPlantilla,
                                       RepositorioCampoPlantilla repositorioCampoPlantilla) {
        this.repositorioPlantilla = repositorioPlantilla;
        this.repositorioVersionPlantilla = repositorioVersionPlantilla;
        this.repositorioCampoPlantilla = repositorioCampoPlantilla;
    }

    public List<CatalogoPlantillaDto> buscarPlantillasActivas(TipoContrato tipo) {
        List<Plantilla> plantillas = tipo != null
                ? repositorioPlantilla.findByEstadoAndTipoContrato(EstadoPlantilla.ACTIVA, tipo)
                : repositorioPlantilla.findByEstado(EstadoPlantilla.ACTIVA);
        return plantillas.stream()
                .filter(plantilla -> plantilla.getIdVersionActual() != null)
                .map(plantilla -> new CatalogoPlantillaDto(
                        plantilla.getId(), plantilla.getIdVersionActual(), plantilla.getNombre(),
                        plantilla.getTipoContrato(), plantilla.getDescripcion()))
                .toList();
    }

    public FormularioPlantillaDto obtenerFormulario(UUID idPlantilla, UUID idVersion) {
        Plantilla plantilla = repositorioPlantilla.findById(idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La plantilla no existe."));
        if (plantilla.getEstado() != EstadoPlantilla.ACTIVA) {
            throw new ConflictoEstadoException("La plantilla ya no esta activa.");
        }
        VersionPlantilla version = repositorioVersionPlantilla.findByIdAndPlantillaId(idVersion, idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version de plantilla no existe."));

        List<CampoDinamicoDto> campos = repositorioCampoPlantilla
                .findByVersionPlantillaIdOrderByOrdenVisualAsc(version.getId()).stream()
                .map(campo -> new CampoDinamicoDto(
                        campo.getNombreMarcador(), campo.getEtiqueta(), campo.getTipoDato(),
                        campo.isObligatorio(), campo.getValorPredeterminado()))
                .toList();

        return new FormularioPlantillaDto(idPlantilla, version.getId(), plantilla.getNombre(),
                plantilla.getTipoContrato(), campos);
    }

    /** Uso interno de ServicioGestionContratos (CU-01): version activa con su contenido y campos completos. */
    public VersionPlantilla obtenerVersionActivaParaCreacion(UUID idPlantilla, UUID idVersion) {
        Plantilla plantilla = repositorioPlantilla.findById(idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La plantilla no existe."));
        if (plantilla.getEstado() != EstadoPlantilla.ACTIVA) {
            throw new ConflictoEstadoException("La plantilla ya no esta activa.");
        }
        return repositorioVersionPlantilla.findByIdAndPlantillaId(idVersion, idPlantilla)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version de plantilla no existe."));
    }

    public List<CampoPlantilla> obtenerCampos(UUID idVersionPlantilla) {
        return repositorioCampoPlantilla.findByVersionPlantillaIdOrderByOrdenVisualAsc(idVersionPlantilla);
    }
}
