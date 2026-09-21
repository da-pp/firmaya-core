package com.firmaya.api.contratos;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServicioRegistroAuditoria;
import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.contratos.dto.SolicitudVerificarIntegridad;
import com.firmaya.api.contratos.dto.VerificacionIntegridadDto;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-13, EP-36. Recalcula el SHA-256 del contenido persistido y lo compara primero con el
 * hash almacenado y luego con el aportado por el usuario; nunca declara integridad
 * verificada solo por coincidencia entre dos valores ya guardados.
 *
 * Alcance de autorizacion: dado que CU-03/CU-04 (participantes e invitaciones externas)
 * estan fuera de este proyecto, la verificacion queda restringida al Responsable interno
 * del contrato. Ampliar a Firmante/Revisor/Solo lectura y a sesiones externas de consulta
 * requiere esos casos de uso.
 *
 * Canonizacion del contenido: [PENDIENTE] en la especificacion (PD-07). Se recalcula sobre
 * los bytes UTF-8 exactos del campo `contenido` tal como esta persistido, sin normalizacion
 * adicional; es una decision tecnica provisional, documentada para revision.
 */
@Service
@Transactional
public class ServicioIntegridadContrato {

    private final RepositorioContrato repositorioContrato;
    private final RepositorioVersionContrato repositorioVersionContrato;
    private final ServicioRegistroAuditoria servicioRegistroAuditoria;
    private final ServicioHashContenidoContrato servicioHashContenidoContrato;

    public ServicioIntegridadContrato(RepositorioContrato repositorioContrato,
                                       RepositorioVersionContrato repositorioVersionContrato,
                                       ServicioRegistroAuditoria servicioRegistroAuditoria,
                                       ServicioHashContenidoContrato servicioHashContenidoContrato) {
        this.repositorioContrato = repositorioContrato;
        this.repositorioVersionContrato = repositorioVersionContrato;
        this.servicioRegistroAuditoria = servicioRegistroAuditoria;
        this.servicioHashContenidoContrato = servicioHashContenidoContrato;
    }

    public VerificacionIntegridadDto verificar(UUID idContrato, UUID idUsuarioSolicitante,
                                                SolicitudVerificarIntegridad solicitud) {
        Contrato contrato = repositorioContrato.findById(idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));

        if (!contrato.getResponsable().getId().equals(idUsuarioSolicitante)) {
            throw new AccesoDenegadoNegocioException("No tiene acceso a este contrato.");
        }

        UUID idVersionObjetivo = solicitud.idVersion() != null ? solicitud.idVersion() : contrato.getIdVersionActual();
        if (idVersionObjetivo == null) {
            throw new RecursoNoEncontradoException("El contrato no tiene una version disponible para verificar.");
        }
        VersionContrato version = repositorioVersionContrato.findByIdAndContratoId(idVersionObjetivo, idContrato)
                .orElseThrow(() -> new RecursoNoEncontradoException("La version indicada no existe para este contrato."));

        String hashRecalculado = servicioHashContenidoContrato.calcular(version.getContenido());
        String hashAlmacenado = version.getHashSha256().toLowerCase(Locale.ROOT);
        String hashProporcionado = solicitud.hashProporcionado().toLowerCase(Locale.ROOT);

        boolean integridadAlmacenamiento = hashRecalculado.equals(hashAlmacenado);
        boolean coincideHashProporcionado = hashRecalculado.equals(hashProporcionado);
        OffsetDateTime ahora = OffsetDateTime.now();

        servicioRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(contrato.getResponsable(), "VERIFICACION_INTEGRIDAD", "VersionContrato",
                        "Verificacion de integridad: almacenamiento=" + integridadAlmacenamiento
                                + ", coincideAportado=" + coincideHashProporcionado + ".")
                .conVersionContrato(idContrato, version.getId())
                .conHash(hashRecalculado));

        return new VerificacionIntegridadDto(
                version.getId(),
                version.getNumeroVersion(),
                hashRecalculado,
                hashAlmacenado,
                hashProporcionado,
                integridadAlmacenamiento,
                coincideHashProporcionado,
                ahora);
    }
}
