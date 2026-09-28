package com.universoempaques.model;

/**
 * Estados de una cotizacion (columna "Estado" de la tabla Cotizacion).
 * Flujo (RF-06 a RF-10):
 *   SOLICITADA -> (comercial pone el valor) -> COTIZADA
 *   COTIZADA   -> (cliente responde)        -> APROBADA o RECHAZADA
 *   APROBADA   -> (comercial genera el pedido; queda enlazado en CodigoPedido)
 */
public enum EstadoCotizacionTipo {
    SOLICITADA("Solicitada", "status-cotizado"),
    COTIZADA("Cotizada", "status-produccion"),
    APROBADA("Aprobada", "status-terminado"),
    RECHAZADA("Rechazada", "status-rechazado");

    private final String etiqueta;
    private final String claseCss;

    EstadoCotizacionTipo(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    /** Texto para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Clase CSS del "pill" de color (ver estilo.css). */
    public String getClaseCss() {
        return claseCss;
    }
}
