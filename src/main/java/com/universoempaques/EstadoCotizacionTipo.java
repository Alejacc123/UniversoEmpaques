package com.universoempaques;

/**
 * Estados por los que pasa una cotizacion (ver Diagrama de Clases,
 * enumeracion EstadoCotizacion). Se guarda como texto en la columna
 * "estado" de la tabla cotizacion.
 *
 * SOLICITADA -> el cliente la pidio (RF-06).
 * REGISTRADA -> el area comercial registro valor y condiciones (RF-07).
 * APROBADA / RECHAZADA -> respuesta del cliente (RF-08).
 */
public enum EstadoCotizacionTipo {
    SOLICITADA,
    REGISTRADA,
    APROBADA,
    RECHAZADA
};

