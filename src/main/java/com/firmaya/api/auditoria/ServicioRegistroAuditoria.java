package com.firmaya.api.auditoria;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio interno transversal (no expuesto como endpoint): registra eventos de auditoria
 * desde los demas casos de uso.
 *
 * Propagacion REQUIRED (por defecto), uniendose a la transaccion del llamador, a proposito:
 * un evento de auditoria referencia por FK al mismo Usuario que la operacion de negocio
 * suele modificar en la misma llamada (por ejemplo, intentos_inicio_fallidos en login o
 * hash_contrasena en recuperacion). Con REQUIRES_NEW, esa insercion abriria una conexion
 * JDBC distinta que necesita un lock compartido sobre la fila de Usuario para validar la
 * FK, mientras la transaccion externa aun sostiene el lock exclusivo de su propio UPDATE
 * sin comprometer todavia: eso autobloquea la misma solicitud entre dos conexiones. Con
 * REQUIRED, todo ocurre en la misma conexion/transaccion y no hay conflicto de locks;
 * como efecto secundario, si la operacion de negocio revierte, el evento de auditoria
 * revierte con ella (aceptable: preferible a un evento de auditoria inconsistente con un
 * cambio que nunca se aplico).
 */
@Service
public class ServicioRegistroAuditoria {

    private final RepositorioEventoAuditoria repositorioEventoAuditoria;

    public ServicioRegistroAuditoria(RepositorioEventoAuditoria repositorioEventoAuditoria) {
        this.repositorioEventoAuditoria = repositorioEventoAuditoria;
    }

    @Transactional
    public void registrar(RegistroAuditoriaComando comando) {
        EventoAuditoria evento = new EventoAuditoria(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                comando.getTipoActor(),
                comando.getUsuarioActor(),
                comando.getTipoAccion(),
                comando.getTipoEntidad(),
                comando.getIdEntidad(),
                comando.getIdContrato(),
                comando.getIdVersionContrato(),
                comando.getDescripcion(),
                comando.getDatosAnterioresJson(),
                comando.getDatosPosterioresJson(),
                comando.getHashSha256(),
                comando.getDireccionIp(),
                comando.getIdentificadorTraza()
        );
        repositorioEventoAuditoria.save(evento);
    }
}
