package com.firmaya.api.usuarios;

import com.firmaya.api.auditoria.RegistroAuditoriaComando;
import com.firmaya.api.auditoria.ServiceRegistroAuditoria;
import com.firmaya.api.comun.PaginaDto;
import com.firmaya.api.comun.excepciones.ConflictoEstadoException;
import com.firmaya.api.comun.excepciones.RecursoNoEncontradoException;
import com.firmaya.api.comun.excepciones.SolicitudInvalidaException;
import com.firmaya.api.notificaciones.ServiceEnvioNotificaciones;
import com.firmaya.api.seguridad.RepositorySesionInterna;
import com.firmaya.api.seguridad.ServiceHashCredencial;
import com.firmaya.api.tokens.PropositoToken;
import com.firmaya.api.tokens.RepositoryTokenAcceso;
import com.firmaya.api.tokens.TokenAcceso;
import com.firmaya.api.usuarios.dto.ReenvioActivacionDto;
import com.firmaya.api.usuarios.dto.SolicitudActualizarUsuario;
import com.firmaya.api.usuarios.dto.SolicitudCambiarEstadoUsuario;
import com.firmaya.api.usuarios.dto.SolicitudCrearUsuario;
import com.firmaya.api.usuarios.dto.UsuarioAdministracionDto;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-15, EP-13..EP-18: alta, consulta, edicion, cambio de estado y reenvio de activacion de
 * cuentas internas. La proteccion del "ultimo administrador" (PD del propio CU-15, sin regla
 * cerrada) se implementa aqui como decision tecnica provisional: se rechaza cualquier cambio
 * de rol o desactivacion que deje al sistema sin ningun Administrador ACTIVO.
 */
@Service
@Transactional
public class ServiceAdministracionUsuarios {

    private static final int TAMANO_PAGINA = 20;

    private final RepositoryUsuario repositoryUsuario;
    private final RepositoryTokenAcceso repositoryTokenAcceso;
    private final RepositorySesionInterna repositorySesionInterna;
    private final ServiceHashCredencial serviceHashCredencial;
    private final ServiceEnvioNotificaciones serviceEnvioNotificaciones;
    private final ServiceRegistroAuditoria serviceRegistroAuditoria;
    private final int duracionHoras;
    private final String urlActivacion;

    public ServiceAdministracionUsuarios(RepositoryUsuario repositoryUsuario,
                                           RepositoryTokenAcceso repositoryTokenAcceso,
                                           RepositorySesionInterna repositorySesionInterna,
                                           ServiceHashCredencial serviceHashCredencial,
                                           ServiceEnvioNotificaciones serviceEnvioNotificaciones,
                                           ServiceRegistroAuditoria serviceRegistroAuditoria,
                                           @Value("${firmaya.activacion.duracion-horas}") int duracionHoras,
                                           @Value("${firmaya.frontend.url-activacion-cuenta}") String urlActivacion) {
        this.repositoryUsuario = repositoryUsuario;
        this.repositoryTokenAcceso = repositoryTokenAcceso;
        this.repositorySesionInterna = repositorySesionInterna;
        this.serviceHashCredencial = serviceHashCredencial;
        this.serviceEnvioNotificaciones = serviceEnvioNotificaciones;
        this.serviceRegistroAuditoria = serviceRegistroAuditoria;
        this.duracionHoras = duracionHoras;
        this.urlActivacion = urlActivacion;
    }

    @Transactional(readOnly = true)
    public PaginaDto<UsuarioAdministracionDto> buscarUsuarios(String correoElectronico, RolGlobal rol,
                                                                EstadoAdministrativo estado, Integer pagina) {
        int numeroPagina = pagina != null ? Math.max(pagina, 0) : 0;
        var paginado = PageRequest.of(numeroPagina, TAMANO_PAGINA, Sort.by(Sort.Direction.ASC, "nombre"));
        var resultado = repositoryUsuario.buscarUsuarios(correoElectronico, rol, estado, paginado);
        return PaginaDto.desde(resultado.map(UsuarioAdministracionDto::desde));
    }

    @Transactional(readOnly = true)
    public UsuarioAdministracionDto obtenerUsuario(UUID idUsuario) {
        return UsuarioAdministracionDto.desde(obtenerOLanzar(idUsuario));
    }

    public UsuarioAdministracionDto crearUsuario(SolicitudCrearUsuario solicitud) {
        String correoNormalizado = solicitud.correoElectronico().trim().toLowerCase(Locale.ROOT);
        repositoryUsuario.findByCorreoNormalizado(correoNormalizado).ifPresent(existente -> {
            throw new SolicitudInvalidaException("Este correo electronico ya esta registrado en el sistema.",
                    java.util.Map.of("correoElectronico", "Este correo electronico ya esta registrado en el sistema."));
        });

        OffsetDateTime ahora = OffsetDateTime.now();
        Usuario usuario = new Usuario(UUID.randomUUID(), solicitud.nombre(), solicitud.apellido(),
                solicitud.correoElectronico(), solicitud.rolGlobal(), solicitud.estadoAdministrativo(),
                EstadoActivacion.PENDIENTE, ahora);
        usuario = repositoryUsuario.save(usuario);

        String estadoEntrega = "NO_APLICA";
        if (usuario.getEstadoAdministrativo() == EstadoAdministrativo.ACTIVO) {
            estadoEntrega = emitirTokenYEnviarActivacion(usuario, ahora) ? "ENVIADO" : "ERROR";
        }

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("USUARIO_CREADO", "Usuario",
                        "Cuenta creada por el administrador; activacion pendiente (entrega: " + estadoEntrega + ").")
                .conEntidad(usuario.getId()));

        return UsuarioAdministracionDto.desde(usuario);
    }

    public UsuarioAdministracionDto actualizarUsuario(UUID idUsuario, SolicitudActualizarUsuario solicitud) {
        Usuario usuario = obtenerOLanzar(idUsuario);

        String nombre = solicitud.nombre() != null ? solicitud.nombre() : usuario.getNombre();
        String apellido = solicitud.apellido() != null ? solicitud.apellido() : usuario.getApellido();
        String correoElectronico = solicitud.correoElectronico() != null
                ? solicitud.correoElectronico() : usuario.getCorreoElectronico();
        RolGlobal rolGlobal = solicitud.rolGlobal() != null ? solicitud.rolGlobal() : usuario.getRolGlobal();

        if (solicitud.correoElectronico() != null) {
            String correoNormalizado = correoElectronico.trim().toLowerCase(Locale.ROOT);
            repositoryUsuario.findByCorreoNormalizado(correoNormalizado)
                    .filter(otro -> !otro.getId().equals(idUsuario))
                    .ifPresent(otro -> {
                        throw new SolicitudInvalidaException("Este correo electronico ya esta registrado en el sistema.");
                    });
        }
        if (rolGlobal != RolGlobal.ADMINISTRADOR && usuario.getRolGlobal() == RolGlobal.ADMINISTRADOR
                && usuario.getEstadoAdministrativo() == EstadoAdministrativo.ACTIVO && esUltimoAdministradorActivo()) {
            throw new ConflictoEstadoException("No es posible quitar el rol de Administrador al ultimo administrador activo.");
        }

        usuario.actualizarDatos(nombre, apellido, correoElectronico, rolGlobal, OffsetDateTime.now());

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("USUARIO_ACTUALIZADO", "Usuario", "Datos de usuario actualizados por el administrador.")
                .conEntidad(usuario.getId()));

        return UsuarioAdministracionDto.desde(usuario);
    }

    public UsuarioAdministracionDto cambiarEstadoAdministrativo(UUID idUsuario, SolicitudCambiarEstadoUsuario solicitud) {
        Usuario usuario = obtenerOLanzar(idUsuario);
        if (usuario.getEstadoAdministrativo() == solicitud.estadoAdministrativo()) {
            throw new ConflictoEstadoException("El usuario ya se encuentra en el estado solicitado.");
        }
        if (solicitud.estadoAdministrativo() == EstadoAdministrativo.INACTIVO
                && usuario.getRolGlobal() == RolGlobal.ADMINISTRADOR && esUltimoAdministradorActivo()) {
            throw new ConflictoEstadoException("No es posible desactivar al ultimo administrador activo.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.cambiarEstadoAdministrativo(solicitud.estadoAdministrativo(), ahora);
        if (solicitud.estadoAdministrativo() == EstadoAdministrativo.INACTIVO) {
            repositorySesionInterna.findVigentesPorUsuario(idUsuario).forEach(sesion -> sesion.revocar(ahora));
        }

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("USUARIO_CAMBIO_ESTADO", "Usuario",
                        "Estado administrativo cambiado a " + solicitud.estadoAdministrativo()
                                + (solicitud.motivo() != null ? ": " + solicitud.motivo() : "."))
                .conEntidad(usuario.getId()));

        return UsuarioAdministracionDto.desde(usuario);
    }

    public ReenvioActivacionDto reenviarActivacion(UUID idUsuario) {
        Usuario usuario = obtenerOLanzar(idUsuario);
        if (usuario.getEstadoActivacion() == EstadoActivacion.COMPLETADA) {
            throw new ConflictoEstadoException("La activacion de este usuario ya fue completada.");
        }
        if (usuario.getEstadoAdministrativo() != EstadoAdministrativo.ACTIVO) {
            throw new ConflictoEstadoException("El usuario no esta administrativamente habilitado.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        boolean enviado = emitirTokenYEnviarActivacion(usuario, ahora);

        serviceRegistroAuditoria.registrar(RegistroAuditoriaComando
                .deSistema("ACTIVACION_REENVIADA", "Usuario",
                        "Enlace de activacion reenviado (entrega: " + (enviado ? "ENVIADO" : "ERROR") + ").")
                .conEntidad(usuario.getId()));

        return new ReenvioActivacionDto(usuario.getId(), ahora.plusHours(duracionHoras), enviado ? "ENVIADO" : "ERROR");
    }

    private boolean emitirTokenYEnviarActivacion(Usuario usuario, OffsetDateTime ahora) {
        repositoryTokenAcceso
                .findByUsuarioIdAndPropositoAndFechaConsumoIsNullAndFechaRevocacionIsNull(
                        usuario.getId(), PropositoToken.ACTIVACION)
                .forEach(previo -> previo.revocar(ahora, "Reemplazado por un nuevo enlace de activacion."));

        String credencialEnClaro = serviceHashCredencial.generarCredencialAleatoria();
        byte[] hash = serviceHashCredencial.hashear(credencialEnClaro);
        OffsetDateTime expiracion = ahora.plusHours(duracionHoras);
        TokenAcceso token = TokenAcceso.deActivacion(UUID.randomUUID(), usuario, hash, ahora, expiracion);
        repositoryTokenAcceso.save(token);

        String enlace = urlActivacion + "?token=" + credencialEnClaro;
        String cuerpoCorreo = "Se creo una cuenta para vos en FirmaYA. Activala y define tu contrasena con el "
                + "siguiente enlace (valido " + duracionHoras + " horas): " + enlace;
        String resumenParaRegistro = "Se envio un enlace de activacion de cuenta, valido " + duracionHoras + " horas.";
        return serviceEnvioNotificaciones.enviarCorreoOperativo(usuario, "ACTIVACION_CUENTA",
                "Activa tu cuenta - FirmaYA", cuerpoCorreo, resumenParaRegistro);
    }

    private boolean esUltimoAdministradorActivo() {
        return repositoryUsuario.countByRolGlobalAndEstadoAdministrativo(
                RolGlobal.ADMINISTRADOR, EstadoAdministrativo.ACTIVO) <= 1;
    }

    private Usuario obtenerOLanzar(UUID idUsuario) {
        return repositoryUsuario.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
