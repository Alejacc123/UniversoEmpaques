package com.universoempaques.service;

import com.universoempaques.model.EstadoPedidoTipo;

/**
 * Evento "el pedido cambio de estado" (patron Observer, RF-16).
 *
 * PedidoService lo PUBLICA cada vez que un pedido se crea o avanza, sin
 * saber quien lo escucha. NotificacionService lo ESCUCHA y envia el
 * correo al cliente. Si manana hay que avisar tambien por WhatsApp o
 * guardar una auditoria, se agrega otro "observador" sin tocar PedidoService.
 *
 * @param codigoPedido  pedido que cambio
 * @param nuevoEstado   estado al que paso
 * @param reporte       observacion que escribio el area (puede ser null)
 */
public record EstadoPedidoCambiadoEvent(Integer codigoPedido, EstadoPedidoTipo nuevoEstado, String reporte) {
}
