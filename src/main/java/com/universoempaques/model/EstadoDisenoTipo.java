package com.universoempaques.model;

/**
 * Estado de cada VERSION de un diseno (columna "Estado" de la tabla Diseno).
 * RF-12 (Diseno la carga) y RF-13 (se aprueba o se piden ajustes).
 *
 *   PENDIENTE -> APROBADO            (listo para produccion)
 *   PENDIENTE -> AJUSTE_SOLICITADO   (Diseno sube una nueva version)
 */
public enum EstadoDisenoTipo {
    PENDIENTE("Pendiente de revisión", "status-produccion"),
    APROBADO("Aprobado", "status-terminado"),
    AJUSTE_SOLICITADO("Ajuste solicitado", "status-rechazado");

    private final String etiqueta;
    private final String claseCss;

    EstadoDisenoTipo(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return claseCss;
    }
}
