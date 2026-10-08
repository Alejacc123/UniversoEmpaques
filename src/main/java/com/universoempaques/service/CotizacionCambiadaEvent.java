package com.universoempaques.service;

import com.universoempaques.model.EstadoCotizacionTipo;

/**
 * Evento "la cotizacion cambio de estado" (patron Observer, igual que
 * EstadoPedidoCambiadoEvent). CotizacionService lo publica y
 * NotificacionService avisa por correo a quien le toca responder:
 *   COTIZADA      -> al cliente (ya tiene valor)
 *   CONTRAOFERTA  -> al comercial (el cliente propuso otro valor)
 *   APROBADA / RECHAZADA -> al comercial (y al cliente si la aprobo Comercial)
 *
 * @param codigoCotizacion cotizacion que cambio
 * @param nuevoEstado      estado al que paso
 * @param porComercial     true si el cambio lo hizo Comercial (ej. aceptar la contraoferta)
 */
public record CotizacionCambiadaEvent(Integer codigoCotizacion, EstadoCotizacionTipo nuevoEstado, boolean porComercial) {
}
