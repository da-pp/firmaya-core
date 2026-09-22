package com.firmaya.api.plantillas;

import com.firmaya.api.contratos.TipoContrato;
import com.firmaya.api.plantillas.dto.CatalogoPlantillaDto;
import com.firmaya.api.plantillas.dto.FormularioPlantillaDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU-01/CU-16, EP-19 y EP-20. Requiere sesion interna autenticada (regla general de SeguridadConfig). */
@RestController
public class ControllerCatalogoPlantillas {

    private final ServiceCatalogoPlantillas serviceCatalogoPlantillas;

    public ControllerCatalogoPlantillas(ServiceCatalogoPlantillas serviceCatalogoPlantillas) {
        this.serviceCatalogoPlantillas = serviceCatalogoPlantillas;
    }

    @GetMapping("/api/v1/plantillas")
    public List<CatalogoPlantillaDto> buscarPlantillasActivas(@RequestParam(required = false) TipoContrato tipo) {
        return serviceCatalogoPlantillas.buscarPlantillasActivas(tipo);
    }

    @GetMapping("/api/v1/plantillas/{idPlantilla}/versiones/{idVersion}/formulario")
    public FormularioPlantillaDto obtenerFormulario(@PathVariable UUID idPlantilla, @PathVariable UUID idVersion) {
        return serviceCatalogoPlantillas.obtenerFormulario(idPlantilla, idVersion);
    }
}
