package com.firmaya.api.contratos;

import com.firmaya.api.comun.excepciones.AccesoDenegadoNegocioException;
import com.firmaya.api.participantes.Participante;
import com.firmaya.api.participantes.RepositoryParticipante;
import com.firmaya.api.participantes.RolParticipacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Determina el rol efectivo de un usuario interno sobre un contrato: Responsable, un rol de
 * participacion (si tiene una fila Participante asociada a su cuenta) o ninguno. Compartido
 * por CU-02/04/05/06/07/08/13. El acceso externo (sesion via token) se resuelve directamente
 * desde ContextoParticipanteAutenticado, sin pasar por aqui.
 */
@Component
public class ServiceAutorizacionContrato {

    private final RepositoryParticipante repositoryParticipante;

    public ServiceAutorizacionContrato(RepositoryParticipante repositoryParticipante) {
        this.repositoryParticipante = repositoryParticipante;
    }

    public boolean esResponsable(Contrato contrato, UUID idUsuario) {
        return contrato.esResponsable(idUsuario);
    }

    public Optional<RolParticipacion> rolParticipacionInterna(UUID idContrato, UUID idUsuario) {
        return repositoryParticipante.findByContratoId(idContrato).stream()
                .filter(p -> p.getUsuarioInterno() != null && p.getUsuarioInterno().getId().equals(idUsuario))
                .map(Participante::getRolParticipacion)
                .findFirst();
    }

    /** Responsable o cualquier participante interno con acceso (lectura minima). */
    public void verificarAccesoLectura(Contrato contrato, UUID idUsuario) {
        if (esResponsable(contrato, idUsuario) || rolParticipacionInterna(contrato.getId(), idUsuario).isPresent()) {
            return;
        }
        throw new AccesoDenegadoNegocioException("No tiene acceso a este contrato.");
    }

    public void verificarResponsable(Contrato contrato, UUID idUsuario) {
        if (!esResponsable(contrato, idUsuario)) {
            throw new AccesoDenegadoNegocioException("Solo el responsable del contrato puede realizar esta operacion.");
        }
    }
}
