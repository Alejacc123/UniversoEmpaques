package com.universoempaques.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba el flujo de estados del pedido (patron State en EstadoPedidoTipo). */
class EstadoPedidoTipoTest {

    @Test
    void elFlujoVaEnOrden() {
        assertEquals(EstadoPedidoTipo.EN_DISENO, EstadoPedidoTipo.SOLICITADO.siguiente());
        assertEquals(EstadoPedidoTipo.EN_PRODUCCION, EstadoPedidoTipo.EN_DISENO.siguiente());
        assertEquals(EstadoPedidoTipo.TERMINADO, EstadoPedidoTipo.EN_PRODUCCION.siguiente());
        assertEquals(EstadoPedidoTipo.DESPACHADO, EstadoPedidoTipo.TERMINADO.siguiente());
        assertEquals(EstadoPedidoTipo.ENTREGADO, EstadoPedidoTipo.DESPACHADO.siguiente());
        assertNull(EstadoPedidoTipo.ENTREGADO.siguiente());
    }

    @Test
    void cadaEtapaTieneSuAreaResponsable() {
        assertEquals("COMERCIAL", EstadoPedidoTipo.SOLICITADO.getAreaResponsable());
        assertEquals("DISENO", EstadoPedidoTipo.EN_DISENO.getAreaResponsable());
        assertEquals("PRODUCCION", EstadoPedidoTipo.EN_PRODUCCION.getAreaResponsable());
        assertEquals("BODEGA", EstadoPedidoTipo.TERMINADO.getAreaResponsable());
        assertEquals("BODEGA", EstadoPedidoTipo.DESPACHADO.getAreaResponsable());
        assertNull(EstadoPedidoTipo.ENTREGADO.getAreaResponsable());
    }

    @Test
    void porcentajeDeAvance() {
        assertEquals(0, EstadoPedidoTipo.SOLICITADO.getPorcentaje());
        assertEquals(100, EstadoPedidoTipo.ENTREGADO.getPorcentaje());
        assertTrue(EstadoPedidoTipo.ENTREGADO.esFinal());
    }
}
