package com.firmaya.api.notificaciones;

import com.firmaya.api.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapea firmaya.Notificacion. El destinatario es exclusivamente Usuario interno o
 * Participante externo (id_participante_destinatario se mapea como columna cruda para no
 * introducir una dependencia de paquete hacia participantes; el valor debe coincidir con un
 * Participante.id existente del mismo id_contrato, tal como exige la FK compuesta).
 * NUNCA escribir el enlace tokenizado u OTP en titulo/mensaje.
 */
@Entity
@Table(name = "Notificacion", schema = "firmaya")
public class Notificacion {

    @Id
    @Column(name = "id_notificacion")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEventoNotificacion tipoEvento;

    @Column(name = "id_contrato")
    private UUID idContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_destinatario")
    private Usuario usuarioDestinatario;

    @Column(name = "id_participante_destinatario")
    private UUID idParticipanteDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 18)
    private CanalNotificacion canal;

    @Column(name = "correo_destino", length = 254)
    private String correoDestino;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 2000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 10)
    private EstadoEntregaNotificacion estadoEntrega;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_envio")
    private OffsetDateTime fechaUltimoEnvio;

    @Column(name = "fecha_lectura")
    private OffsetDateTime fechaLectura;

    @Column(name = "referencia_idempotencia", length = 100)
    private String referenciaIdempotencia;

    protected Notificacion() {
    }

    public static Notificacion deCorreo(UUID id, TipoEventoNotificacion tipoEvento, Usuario destinatario,
                                         String correoDestino, String titulo, String mensaje, OffsetDateTime ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.id = id;
        notificacion.tipoEvento = tipoEvento;
        notificacion.usuarioDestinatario = destinatario;
        notificacion.canal = CanalNotificacion.CORREO_ELECTRONICO;
        notificacion.correoDestino = correoDestino;
        notificacion.titulo = titulo;
        notificacion.mensaje = mensaje;
        notificacion.estadoEntrega = EstadoEntregaNotificacion.PENDIENTE;
        notificacion.fechaCreacion = ahora;
        return notificacion;
    }

    public static Notificacion deCorreoParticipante(UUID id, TipoEventoNotificacion tipoEvento, UUID idContrato,
                                                      UUID idParticipante, String correoDestino, String titulo,
                                                      String mensaje, OffsetDateTime ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.id = id;
        notificacion.tipoEvento = tipoEvento;
        notificacion.idContrato = idContrato;
        notificacion.idParticipanteDestinatario = idParticipante;
        notificacion.canal = CanalNotificacion.CORREO_ELECTRONICO;
        notificacion.correoDestino = correoDestino;
        notificacion.titulo = titulo;
        notificacion.mensaje = mensaje;
        notificacion.estadoEntrega = EstadoEntregaNotificacion.PENDIENTE;
        notificacion.fechaCreacion = ahora;
        return notificacion;
    }

    public static Notificacion dePlataforma(UUID id, TipoEventoNotificacion tipoEvento, Usuario destinatario,
                                             String titulo, String mensaje, OffsetDateTime ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.id = id;
        notificacion.tipoEvento = tipoEvento;
        notificacion.usuarioDestinatario = destinatario;
        notificacion.canal = CanalNotificacion.PLATAFORMA;
        notificacion.titulo = titulo;
        notificacion.mensaje = mensaje;
        notificacion.estadoEntrega = EstadoEntregaNotificacion.ENVIADA;
        notificacion.fechaCreacion = ahora;
        notificacion.fechaUltimoEnvio = ahora;
        return notificacion;
    }

    public UUID getId() {
        return id;
    }

    public TipoEventoNotificacion getTipoEvento() {
        return tipoEvento;
    }

    public Usuario getUsuarioDestinatario() {
        return usuarioDestinatario;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public String getCorreoDestino() {
        return correoDestino;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public EstadoEntregaNotificacion getEstadoEntrega() {
        return estadoEntrega;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void marcarEnviada(OffsetDateTime ahora) {
        this.estadoEntrega = EstadoEntregaNotificacion.ENVIADA;
        this.fechaUltimoEnvio = ahora;
    }

    public void marcarError(OffsetDateTime ahora) {
        this.estadoEntrega = EstadoEntregaNotificacion.ERROR;
        this.fechaUltimoEnvio = ahora;
    }

    public void marcarLeida(OffsetDateTime ahora) {
        if (this.fechaLectura == null) {
            this.fechaLectura = ahora;
        }
    }
}
