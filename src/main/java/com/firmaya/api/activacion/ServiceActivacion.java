package com.firmaya.api.activacion;

import com.firmaya.api.activacion.dto.ResultadoActivacionDto;
import com.firmaya.api.activacion.dto.SolicitudCompletarActivacion;
import com.firmaya.api.activacion.dto.ValidacionActivacionDto;
import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.comun.excepciones.TokenExpiradoException;
import com.firmaya.api.comun.excepciones.TokenInvalidoException;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.PropositoToken;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.EstadoActivacion;
import com.firmaya.api.usuarios.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-15, EP-04 y EP-05: validacion y completado del enlace de activacion de 24 horas emitido
 * por {@code ServiceAdministracionUsuarios} al crear una cuenta habilitada. La activacion NO
 * revierte una desactivacion administrativa posterior (Alternativa 5 de CU-15): completar el
 * token siempre establece la contrasena, pero el inicio de sesion sigue bloqueado mientras
 * estadoAdministrativo != ACTIVO (ver Usuario.habilitadaParaIniciarSesion()).
 */
@Service
@Transactional
public class ServiceActivacion {

    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final ServiceHashCredencial serviceHashCredencial;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;

    public ServiceActivacion(RepositoryTokenAcceso repositoryTokenAcceso,
                               ServiceHashCredencial serviceHashCredencial,
                               PasswordEncoder passwordEncoder,
                               ServiceRegistroAuditoria serviceRegistroAuditoria) {
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.serviceHashCredencial = serviceHashCredencial;
        this.passwordEncoder = passwordEncoder;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
    }

    @Transactional(readOnly = true)
    public ValidacionActivacionDto validarToken(String tokenPlano) {
        TokenAcceso token = buscarTokenVigenteOLanzar(tokenPlano);
        return new ValidacionActivacionDto(true, token.getFechaExpiracion());
    }

    public ResultadoActivacionDto completarActivacion(SolicitudCompletarActivacion solicitud) {
        if (!solicitud.contrasenaNueva().equals(solicitud.confirmarContrasena())) {
            throw new SolicitudInvalidaException("Las contrasenas no coinciden.");
        }

        TokenAcceso token = buscarTokenVigenteOLanzar(solicitud.token());
        Usuario usuario = token.getUsuario();
        if (usuario.getEstadoActivacion() == EstadoActivacion.COMPLETADA) {
            throw new ConflictoEstadoException("La activacion de esta cuenta ya fue completada.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.completarActivacion(passwordEncoder.encode(solicitud.contrasenaNueva()), ahora);
        token.consumir(ahora);

        List<TokenAcceso> otrosVigentes = repositoryTokenAcceso
                .findByUsuarioIdAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
                        usuario.getId(), PropositoToken.ACTIVACION);
        otrosVigentes.stream()
                .filter(otro -> !otro.getId().equals(token.getId()))
                .forEach(otro -> otro.revocar(ahora, "Reemplazado por una activacion exitosa."));

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deUsuario(usuario, "CUENTA_ACTIVADA", "Usuario", "Activacion de cuenta completada por el usuario.")
                .conEntidad(usuario.getId()));

        return new ResultadoActivacionDto(true, usuario.getId());
    }

    private TokenAcceso buscarTokenVigenteOLanzar(String tokenPlano) {
        byte[] hash = serviceHashCredencial.hashear(tokenPlano);
        TokenAcceso token = repositoryTokenAcceso.findByHashTokenAndProposito(hash, PropositoToken.ACTIVACION)
                .orElseThrow(() -> new TokenInvalidoException("El enlace de activacion no es valido."));
        if (!token.estaVigente(OffsetDateTime.now())) {
            throw new TokenExpiradoException("El enlace de activacion ha expirado o no es valido.");
        }
        return token;
    }
}
