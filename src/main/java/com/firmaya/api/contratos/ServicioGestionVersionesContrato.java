package com.firmaya.api.contratos;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.contratos.dto.SolicitudGuardarVersionContrato;
import com.firmaya.api.contratos.dto.SolicitudRestaurarVersionContrato;
import com.firmaya.api.contratos.dto.VersionContratoDto;
import com.firmaya.api.usuarios.RepositorioUsuario;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-02, EP-33: guarda una nueva version por edicion. Control de concurrencia optimista via
 * idVersionBase: si no coincide con la version actual persistida, se rechaza sin sobrescribir
 * (409), tal como exige EP-33 y la Alternativa 4 de CU-02.
 *
 * CU-14, EP-35: restaura el contenido de una version historica como una nueva version
 * consecutiva (nunca retrocede el numero ni modifica versiones existentes).
 */
@Service
@Transactional
public class ServicioGestionVersionesContrato {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioAutorizacionContrato servicioAutorizacionContrato;
    private final ServicioHashContenidoContrato servicioHashContenidoContrato;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;

    public ServicioGestionVersionesContrato(RepositorioContrato repositorioContrato,
                                             RepositorioVersionContrato repositorioVersionContrato,
                                             RepositorioUsuario repositorioUsuario,
                                             ServicioAutorizacionContrato servicioAutorizacionContrato,
                                             ServicioHashContenidoContrato servicioHashContenidoContrato,
                                             ServicioRegistroAuditoria servicioRegistroAuditoria) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioAutorizacionContrato = servicioAutorizacionContrato;
        this.servicioHashContenidoContrato = servicioHashContenidoContrato;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
    }

    public VersionContratoDto guardarNuevaVersion(UUID idContrato, UUID idUsuario,
                                                   SolicitudGuardarVersionContrato solicitud) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        if (!contrato.esEditable()) {
            throw new ConflictoEstadoException("Este contrato no puede ser editado en su estado actual.");
        }
        if (contrato.getIdVersionActual() == null || !contrato.getIdVersionActual().equals(solicitud.idVersionBase())) {
            throw new ConflictoEstadoException(
                    "La version base ya no es la version actual del contrato; recargue antes de guardar.");
        }

        Usuario autor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        VersionContrato versionAnterior = repositorioVersionContrato
                .findTopByContratoIdOrderByNumeroVersionDesc(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no tiene versiones."));

        String hash = servicioHashContenidoContrato.calcular(solicitud.contenido());
        OffsetDateTime ahora = OffsetDateTime.now();
        VersionContrato nuevaVersion = VersionContrato.crear(UUID.randomUUID(), contrato,
                versionAnterior.getNumeroVersion() + 1, solicitud.contenido(), hash, autor, ahora,
                solicitud.comentarioCambio(), null);
        repositorioVersionContrato.save(nuevaVersion);

        contrato.establecerVersionActual(nuevaVersion.getId(), ahora);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(autor, "CONTRATO_VERSION_GUARDADA", "VersionContrato",
                        "Nueva version " + nuevaVersion.getNumeroVersion() + " guardada por edicion.")
                .conVersionContrato(idContrato, nuevaVersion.getId())
                .conHash(hash));

        return VersionContratoDto.desde(nuevaVersion);
    }

    /**
     * CU-14, EP-35: crea una nueva version con el contenido de {@code idVersionOrigen}. La
     * version origen debe ser anterior a la actual (Alternativa 3: la version actual no se
     * "restaura" sobre si misma) y {@code idVersionBase} debe seguir coincidiendo con la
     * version actual persistida al confirmar (Alternativa 4: rechaza si hubo edicion
     * concurrente). El hash se recalcula sobre el contenido restaurado; al ser los mismos
     * bytes que el origen, coincide con el hash original.
     */
    public VersionContratoDto restaurarVersion(UUID idContrato, UUID idVersionOrigen, UUID idUsuario,
                                                SolicitudRestaurarVersionContrato solicitud) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
        servicioAutorizacionContrato.verificarResponsable(contrato, idUsuario);

        if (!contrato.esEditable()) {
            throw new ConflictoEstadoException("La restauracion no esta disponible en el estado actual del contrato.");
        }
        if (contrato.getIdVersionActual() == null || !contrato.getIdVersionActual().equals(solicitud.idVersionBase())) {
            throw new ConflictoEstadoException(
                    "La version actual del contrato cambio mientras se preparaba la restauracion; recargue e intente de nuevo.");
        }
        if (idVersionOrigen.equals(contrato.getIdVersionActual())) {
            throw new SolicitudInvalidaException("Debe elegirse una version anterior a la actual para restaurar.");
        }

        VersionContrato versionOrigen = repositorioVersionContrato.findByIdAndContratoId(idVersionOrigen, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version a restaurar no existe."));
        VersionContrato versionAnterior = repositorioVersionContrato
                .findTopByContratoIdOrderByNumeroVersionDesc(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no tiene versiones."));

        Usuario autor = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        String hash = servicioHashContenidoContrato.calcular(versionOrigen.getContenido());
        OffsetDateTime ahora = OffsetDateTime.now();
        VersionContrato nuevaVersion = VersionContrato.crear(UUID.randomUUID(), contrato,
                versionAnterior.getNumeroVersion() + 1, versionOrigen.getContenido(), hash, autor, ahora,
                solicitud.motivo(), versionOrigen.getId());
        repositorioVersionContrato.save(nuevaVersion);

        contrato.establecerVersionActual(nuevaVersion.getId(), ahora);

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(autor, "CONTRATO_VERSION_RESTAURADA", "VersionContrato",
                        "Version " + versionOrigen.getNumeroVersion() + " restaurada como nueva version "
                                + nuevaVersion.getNumeroVersion() + ".")
                .conVersionContrato(idContrato, nuevaVersion.getId())
                .conHash(hash));

        return VersionContratoDto.desde(nuevaVersion);
    }
}
