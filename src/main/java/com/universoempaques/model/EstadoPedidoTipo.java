package com.universoempaques.model;

/**
 * Estados por los que pasa un pedido, en orden (columna "Estado" de la
 * tabla EstadoPedido). RF-14, RF-15, RF-17.
 *
 * Patron State (version ligera con enum): cada estado sabe
 *   - cual es el SIGUIENTE estado,
 *   - que AREA es responsable de hacerlo avanzar,
 *   - como se llama esa ACCION en pantalla.
 * Asi las reglas del flujo viven en un solo lugar y no en muchos "if".
 *
 *   SOLICITADO --Comercial--> EN_DISENO --Diseno--> EN_PRODUCCION
 *   --Produccion--> TERMINADO --Bodega--> DESPACHADO --Bodega--> ENTREGADO
 */
public enum EstadoPedidoTipo {
    SOLICITADO("Solicitado", "status-cotizado", "COMERCIAL", "Enviar a diseño"),
    EN_DISENO("En diseño", "status-despachado", "DISENO", "Diseño listo: enviar a producción"),
    EN_PRODUCCION("En producción", "status-produccion", "PRODUCCION", "Producción terminada"),
    TERMINADO("Terminado", "status-terminado", "BODEGA", "Registrar despacho"),
    DESPACHADO("Despachado", "status-despachado", "BODEGA", "Registrar entrega"),
    ENTREGADO("Entregado", "status-entregado", null, null);

    private final String etiqueta;
    private final String claseCss;
    private final String areaResponsable;
    private final String accion;

    EstadoPedidoTipo(String etiqueta, String claseCss, String areaResponsable, String accion) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
        this.areaResponsable = areaResponsable;
        this.accion = accion;
    }

    /** Texto para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Clase CSS del "pill" de color (ver estilo.css). */
    public String getClaseCss() {
        return claseCss;
    }

    /** Area que puede pasar el pedido al siguiente estado (null = ya termino). */
    public String getAreaResponsable() {
        return areaResponsable;
    }

    /** Texto del boton que hace avanzar el pedido desde este estado. */
    public String getAccion() {
        return accion;
    }

    public boolean esFinal() {
        return this == ENTREGADO;
    }

    /** El estado que sigue, o null si ya esta ENTREGADO. */
    public EstadoPedidoTipo siguiente() {
        return esFinal() ? null : values()[ordinal() + 1];
    }

    /** Porcentaje de avance para la barra de progreso (0 a 100). */
    public int getPorcentaje() {
        return ordinal() * 100 / (values().length - 1);
    }
}
