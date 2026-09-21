package com.firmaya.api.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea firmaya.Usuario (ver FirmaYA_Creacion_Base_Datos_SQLServer_v1.sql).
 * correo_normalizado es una columna calculada PERSISTED por SQL Server: se lee pero nunca se escribe.
 */
@Entity
@Table(name = "Usuario", schema = "firmaya")
public class Usuario {

    @Id
    @Column(name = "id_usuario")
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "correo_electronico", nullable = false, length = 254)
    private String correoElectronico;

    @Column(name = "correo_normalizado", insertable = false, updatable = false)
    private String correoNormalizado;

    @Column(name = "hash_contrasena", length = 255)
    private String hashContrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_global", nullable = false, length = 24)
    private RolGlobal rolGlobal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_administrativo", nullable = false, length = 8)
    private EstadoAdministrativo estadoAdministrativo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_activacion", nullable = false, length = 12)
    private EstadoActivacion estadoActivacion;

    @Column(name = "intentos_inicio_fallidos", nullable = false)
    private int intentosInicioFallidos;

    @Column(name = "bloqueo_hasta")
    private OffsetDateTime bloqueoHasta;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_activacion")
    private OffsetDateTime fechaActivacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Usuario() {
    }

    public Usuario(UUID id, String nombre, String apellido, String correoElectronico, RolGlobal rolGlobal,
                   EstadoAdministrativo estadoAdministrativo, EstadoActivacion estadoActivacion,
                   OffsetDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.rolGlobal = rolGlobal;
        this.estadoAdministrativo = estadoAdministrativo;
        this.estadoActivacion = estadoActivacion;
        this.intentosInicioFallidos = 0;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaCreacion;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public String getCorreoNormalizado() {
        return correoNormalizado;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public void establecerContrasena(String hashContrasena, OffsetDateTime ahora) {
        this.hashContrasena = hashContrasena;
        this.fechaActualizacion = ahora;
    }

    public RolGlobal getRolGlobal() {
        return rolGlobal;
    }

    public EstadoAdministrativo getEstadoAdministrativo() {
        return estadoAdministrativo;
    }

    public EstadoActivacion getEstadoActivacion() {
        return estadoActivacion;
    }

    public int getIntentosInicioFallidos() {
        return intentosInicioFallidos;
    }

    public OffsetDateTime getBloqueoHasta() {
        return bloqueoHasta;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActivacion() {
        return fechaActivacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public boolean habilitadaParaIniciarSesion() {
        return estadoAdministrativo == EstadoAdministrativo.ACTIVO
                && estadoActivacion == EstadoActivacion.COMPLETADA
                && hashContrasena != null;
    }

    public boolean estaBloqueada(OffsetDateTime ahora) {
        return bloqueoHasta != null && bloqueoHasta.isAfter(ahora);
    }

    public void registrarIntentoFallido(int maximoIntentos, int minutosBloqueo, OffsetDateTime ahora) {
        this.intentosInicioFallidos++;
        if (this.intentosInicioFallidos >= maximoIntentos) {
            this.bloqueoHasta = ahora.plusMinutes(minutosBloqueo);
        }
        this.fechaActualizacion = ahora;
    }

    public void registrarIniciosSesionExitoso(OffsetDateTime ahora) {
        this.intentosInicioFallidos = 0;
        this.bloqueoHasta = null;
        this.fechaActualizacion = ahora;
    }

    /** CU-15, EP-16: solo nombre/apellido/correo/rol; estado y contrasena tienen su propio flujo. */
    public void actualizarDatos(String nombre, String apellido, String correoElectronico, RolGlobal rolGlobal,
                                 OffsetDateTime ahora) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.rolGlobal = rolGlobal;
        this.fechaActualizacion = ahora;
    }

    /** CU-15, EP-17: la invalidacion de sesiones activas la ejecuta el llamador. */
    public void cambiarEstadoAdministrativo(EstadoAdministrativo nuevoEstado, OffsetDateTime ahora) {
        this.estadoAdministrativo = nuevoEstado;
        this.fechaActualizacion = ahora;
    }

    /** CU-15, EP-05: completa la activacion y establece la primera contrasena en un solo paso. */
    public void completarActivacion(String hashContrasena, OffsetDateTime ahora) {
        this.hashContrasena = hashContrasena;
        this.estadoActivacion = EstadoActivacion.COMPLETADA;
        this.fechaActivacion = ahora;
        this.fechaActualizacion = ahora;
    }
}
