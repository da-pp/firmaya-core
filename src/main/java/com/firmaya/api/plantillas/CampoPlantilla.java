package com.firmaya.api.plantillas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Mapea firmaya.CampoPlantilla: definicion de un campo dinamico (marcador) de una version de plantilla. */
@Entity
@Table(name = "CampoPlantilla", schema = "firmaya")
public class CampoPlantilla {

    @Id
    @Column(name = "id_campo_plantilla")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_version_plantilla", nullable = false)
    private VersionPlantilla versionPlantilla;

    @Column(name = "nombre_marcador", nullable = false, length = 100)
    private String nombreMarcador;

    @Column(name = "etiqueta", nullable = false, length = 200)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", nullable = false, length = 8)
    private TipoDatoCampo tipoDato;

    @Column(name = "obligatorio", nullable = false)
    private boolean obligatorio;

    @Column(name = "orden_visual", nullable = false)
    private int ordenVisual;

    @Column(name = "valor_predeterminado")
    private String valorPredeterminado;

    @Column(name = "restricciones_json")
    private String restriccionesJson;

    protected CampoPlantilla() {
    }

    public static CampoPlantilla crear(UUID id, VersionPlantilla versionPlantilla, String nombreMarcador,
                                        String etiqueta, TipoDatoCampo tipoDato, boolean obligatorio,
                                        int ordenVisual, String valorPredeterminado) {
        CampoPlantilla campo = new CampoPlantilla();
        campo.id = id;
        campo.versionPlantilla = versionPlantilla;
        campo.nombreMarcador = nombreMarcador;
        campo.etiqueta = etiqueta;
        campo.tipoDato = tipoDato;
        campo.obligatorio = obligatorio;
        campo.ordenVisual = ordenVisual;
        campo.valorPredeterminado = valorPredeterminado;
        return campo;
    }

    public UUID getId() {
        return id;
    }

    public String getNombreMarcador() {
        return nombreMarcador;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public TipoDatoCampo getTipoDato() {
        return tipoDato;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public int getOrdenVisual() {
        return ordenVisual;
    }

    public String getValorPredeterminado() {
        return valorPredeterminado;
    }

    public String getRestriccionesJson() {
        return restriccionesJson;
    }
}
