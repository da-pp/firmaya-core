package com.firmaya.api.auditoria;

import com.firmaya.api.usuarios.Usuario;
import java.util.UUID;

/** Builder de un evento de auditoria a registrar via {@link ServicioRegistroAuditoria}. */
public final class RegistroAuditoriaComando {

    private TipoActorAuditoria tipoActor = TipoActorAuditoria.SISTEMA;
    private Usuario usuarioActor;
    private String tipoAccion;
    private String tipoEntidad;
    private UUID idEntidad;
    private UUID idContrato;
    private UUID idVersionContrato;
    private String descripcion;
    private String datosAnterioresJson;
    private String datosPosterioresJson;
    private String hashSha256;
    private String direccionIp;
    private String identificadorTraza;

    private RegistroAuditoriaComando() {
    }

    public static RegistroAuditoriaComando deUsuario(Usuario usuario, String tipoAccion, String tipoEntidad, String descripcion) {
        RegistroAuditoriaComando comando = new RegistroAuditoriaComando();
        comando.tipoActor = TipoActorAuditoria.USUARIO;
        comando.usuarioActor = usuario;
        comando.tipoAccion = tipoAccion;
        comando.tipoEntidad = tipoEntidad;
        comando.descripcion = descripcion;
        return comando;
    }

    public static RegistroAuditoriaComando deSistema(String tipoAccion, String tipoEntidad, String descripcion) {
        RegistroAuditoriaComando comando = new RegistroAuditoriaComando();
        comando.tipoAccion = tipoAccion;
        comando.tipoEntidad = tipoEntidad;
        comando.descripcion = descripcion;
        return comando;
    }

    public RegistroAuditoriaComando conEntidad(UUID idEntidad) {
        this.idEntidad = idEntidad;
        return this;
    }

    public RegistroAuditoriaComando conContrato(UUID idContrato) {
        this.idContrato = idContrato;
        return this;
    }

    public RegistroAuditoriaComando conVersionContrato(UUID idContrato, UUID idVersionContrato) {
        this.idContrato = idContrato;
        this.idVersionContrato = idVersionContrato;
        return this;
    }

    public RegistroAuditoriaComando conHash(String hashSha256) {
        this.hashSha256 = hashSha256;
        return this;
    }

    public RegistroAuditoriaComando conIp(String direccionIp) {
        this.direccionIp = direccionIp;
        return this;
    }

    public RegistroAuditoriaComando conDatos(String datosAnterioresJson, String datosPosterioresJson) {
        this.datosAnterioresJson = datosAnterioresJson;
        this.datosPosterioresJson = datosPosterioresJson;
        return this;
    }

    public RegistroAuditoriaComando conTraza(String identificadorTraza) {
        this.identificadorTraza = identificadorTraza;
        return this;
    }

    TipoActorAuditoria getTipoActor() {
        return tipoActor;
    }

    Usuario getUsuarioActor() {
        return usuarioActor;
    }

    String getTipoAccion() {
        return tipoAccion;
    }

    String getTipoEntidad() {
        return tipoEntidad;
    }

    UUID getIdEntidad() {
        return idEntidad;
    }

    UUID getIdContrato() {
        return idContrato;
    }

    UUID getIdVersionContrato() {
        return idVersionContrato;
    }

    String getDescripcion() {
        return descripcion;
    }

    String getDatosAnterioresJson() {
        return datosAnterioresJson;
    }

    String getDatosPosterioresJson() {
        return datosPosterioresJson;
    }

    String getHashSha256() {
        return hashSha256;
    }

    String getDireccionIp() {
        return direccionIp;
    }

    String getIdentificadorTraza() {
        return identificadorTraza;
    }
}
