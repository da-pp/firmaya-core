package com.firmaya.api.contratos;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.dto.DetalleContratoDto;
import com.firmaya.api.contratos.dto.SolicitudCrearContrato;
import com.firmaya.api.contratos.dto.VersionContratoDto;
import com.firmaya.api.plantillas.CampoPlantilla;
import com.firmaya.api.plantillas.ServiceCatalogoPlantillas;
import com.firmaya.api.plantillas.VersionPlantilla;
import com.firmaya.api.usuarios.RepositoryUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-01, EP-29: crea un contrato en BORRADOR a partir de una version de plantilla activa. */
@Service
@Transactional
public class ServiceGestionContratos {

    private final RepositoryContrato repositoryContrato;
    private final RepositoryVersionContrato repositoryVersionContrato;
    private final RepositoryUsuario repositoryUsuario;
    private final ServiceCatalogoPlantillas serviceCatalogoPlantillas;
    private final ServiceHashContenidoContrato serviceHashContenidoContrato;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceGestionContratos(RepositoryContrato repositoryContrato,
                                     RepositoryVersionContrato repositoryVersionContrato,
                                     RepositoryUsuario repositoryUsuario,
                                     ServiceCatalogoPlantillas serviceCatalogoPlantillas,
                                     ServiceHashContenidoContrato serviceHashContenidoContrato,
                                     ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.repositoryContrato = repositoryContrato;
        this.repositoryVersionContrato = repositoryVersionContrato;
        this.repositoryUsuario = repositoryUsuario;
        this.serviceCatalogoPlantillas = serviceCatalogoPlantillas;
        this.serviceHashContenidoContrato = serviceHashContenidoContrato;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    public DetalleContratoDto crearDesdePlantilla(UUID idUsuarioCreador, SolicitudCrearContrato solicitud) {
        Usuario creador = repositoryUsuario.findById(idUsuarioCreador)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        VersionPlantilla versionPlantilla = serviceCatalogoPlantillas
                .obtenerVersionActivaParaCreacion(solicitud.idPlantilla(), solicitud.idVersionPlantilla());
        List<CampoPlantilla> campos = serviceCatalogoPlantillas.obtenerCampos(versionPlantilla.getId());

        String contenido = sustituirMarcadores(versionPlantilla.getContenido(), campos, solicitud.valores());
        String hash = serviceHashContenidoContrato.calcular(contenido);

        OffsetDateTime ahora = OffsetDateTime.now();
        Contrato contrato = Contrato.crear(UUID.randomUUID(), solicitud.nombre(),
                versionPlantilla.getPlantilla().getTipoContrato(), solicitud.partesInvolucradas(),
                solicitud.fechaInicio(), solicitud.fechaExpiracion(), solicitud.descripcionPropiedad(),
                creador, solicitud.idPlantilla(), solicitud.idVersionPlantilla(), ahora);
        // Se reasigna el resultado de save(): al tener el @Id asignado manualmente (UUID
        // propio, sin @GeneratedValue), Spring Data no puede reconocer la entidad como nueva
        // por un id nulo y usa entityManager.merge() en lugar de persist(); merge() devuelve
        // una copia gestionada DISTINTA de la instancia original. Si se sigue mutando la
        // instancia original despues de save(), esos cambios quedan en un objeto no
        // gestionado y nunca se sincronizan con la base de datos. Por eso toda mutacion
        // posterior (aqui, fijar la version actual) debe aplicarse sobre el valor devuelto.
        contrato = repositoryContrato.save(contrato);

        VersionContrato version = VersionContrato.crear(UUID.randomUUID(), contrato, 1, contenido, hash,
                creador, ahora, null, null);
        version = repositoryVersionContrato.save(version);

        contrato.establecerVersionActual(version.getId(), ahora);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(creador, "CONTRATO_CREADO", "Contrato", "Contrato creado desde plantilla.")
                .conVersionContrato(contrato.getId(), version.getId())
                .conHash(hash));

        return new DetalleContratoDto(
                contrato.getId(), contrato.getNombre(), contrato.getTipoContrato(), contrato.getPartesInvolucradas(),
                contrato.getEstado(), contrato.getResponsable().getId(), contrato.getIdVersionPlantilla(),
                VersionContratoDto.desde(version), contrato.getFechaInicio(), contrato.getFechaExpiracion(),
                contrato.getDescripcionPropiedad(), contrato.getFechaCreacion(), List.of("EDITAR", "COMENTAR",
                "INVITAR", "CAMBIAR_ESTADO", "VER_HISTORIAL", "VERIFICAR_INTEGRIDAD"));
    }

    private String sustituirMarcadores(String contenidoPlantilla, List<CampoPlantilla> campos,
                                        Map<String, Object> valores) {
        Map<String, Object> valoresRecibidos = valores != null ? valores : Map.of();
        String contenido = contenidoPlantilla;
        Map<String, String> errores = new LinkedHashMap<>();

        for (CampoPlantilla campo : campos) {
            Object valor = valoresRecibidos.get(campo.getNombreMarcador());
            if (valor == null || String.valueOf(valor).isBlank()) {
                if (campo.isObligatorio()) {
                    errores.put(campo.getNombreMarcador(), "Este campo es obligatorio.");
                }
                contenido = contenido.replace("{{" + campo.getNombreMarcador() + "}}", "");
                continue;
            }
            String valorTexto = validarYFormatear(campo, valor, errores);
            contenido = contenido.replace("{{" + campo.getNombreMarcador() + "}}", valorTexto);
        }

        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Uno o mas campos dinamicos son invalidos.", errores);
        }
        return contenido;
    }

    private String validarYFormatear(CampoPlantilla campo, Object valor, Map<String, String> errores) {
        try {
            return switch (campo.getTipoDato()) {
                case TEXTO -> String.valueOf(valor);
                case NUMERO -> {
                    if (valor instanceof Number numero) {
                        yield String.valueOf(numero);
                    }
                    yield String.valueOf(Double.parseDouble(String.valueOf(valor)));
                }
                case BOOLEANO -> {
                    if (valor instanceof Boolean booleano) {
                        yield booleano ? "Si" : "No";
                    }
                    yield Boolean.parseBoolean(String.valueOf(valor)) ? "Si" : "No";
                }
                case FECHA -> LocalDate.parse(String.valueOf(valor)).toString();
            };
        } catch (NumberFormatException | DateTimeParseException ex) {
            errores.put(campo.getNombreMarcador(), "El valor no tiene el formato esperado para el tipo "
                    + campo.getTipoDato() + ".");
            return "";
        }
    }
}
