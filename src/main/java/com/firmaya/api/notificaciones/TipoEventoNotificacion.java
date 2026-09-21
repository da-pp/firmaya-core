package com.firmaya.api.notificaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Mapea firmaya.TipoEventoNotificacion. Catalogo sembrado en V2 (propuesta, PD-11 pendiente). */
@Entity
@Table(name = "TipoEventoNotificacion", schema = "firmaya")
public class TipoEventoNotificacion {

    @Id
    @Column(name = "id_tipo_evento")
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "etiqueta", nullable = false, length = 150)
    private String etiqueta;

    @Column(name = "configurable", nullable = false)
    private boolean configurable;

    @Column(name = "permite_correo", nullable = false)
    private boolean permiteCorreo;

    @Column(name = "permite_plataforma", nullable = false)
    private boolean permitePlataforma;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected TipoEventoNotificacion() {
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public boolean isPermiteCorreo() {
        return permiteCorreo;
    }

    public boolean isPermitePlataforma() {
        return permitePlataforma;
    }

    public boolean isActivo() {
        return activo;
    }
}
