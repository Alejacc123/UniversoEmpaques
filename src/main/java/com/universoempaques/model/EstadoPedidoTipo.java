package com.universoempaques.model;

/**
 * Estados por los que puede pasar un pedido, en orden.
 * Se guarda como texto en la columna "estado" de la tabla
 * estado_pedido (ver Diccionario de Datos).
 */
public enum EstadoPedidoTipo {
    SOLICITADO,
    EN_DISENO,
    EN_PRODUCCION,
    TERMINADO,
    DESPACHADO,
    ENTREGADO
}
