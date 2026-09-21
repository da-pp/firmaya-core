package com.firmaya.api.participantes;

import com.firmaya.api.contratos.Contrato;
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

/** Mapea firmaya.Participante (CU-03). El Responsable se representa aparte, en Contrato.id_responsable. */
@Entity
@Table(name = "Participante", schema = "firmaya")
public class Participante {

    @Id
    @Column(name = "id_participante")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_interno")
    private Usuario usuarioInterno;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "correo_electronico", nullable = false, length = 254)
    private String correoElectronico;

    @Column(name = "correo_normalizado", insertable = false, updatable = false)
    private String correoNormalizado;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_participacion", nullable = false, length = 12)
    private RolParticipacion rolParticipacion;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    protected Participante() {
    }

    public static Participante crear(UUID id, Contrato contrato, String nombre, String correoElectronico,
                                      RolParticipacion rol, OffsetDateTime ahora) {
        Participante participante = new Participante();
        participante.id = id;
        participante.contrato = contrato;
        participante.nombre = nombre;
        participante.correoElectronico = correoElectronico;
        participante.rolParticipacion = rol;
        participante.fechaCreacion = ahora;
        return participante;
    }

    public UUID getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public Usuario getUsuarioInterno() {
        return usuarioInterno;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public RolParticipacion getRolParticipacion() {
        return rolParticipacion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
