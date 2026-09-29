package com.universoempaques.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Catalogo de productos: el tamano se guarda siempre con el mismo formato. */
class ProductoServiceTest {

    @Test
    void normalizaElTamano() {
        assertEquals("30x20x15 cm", ProductoService.normalizarTamano("30 X 20 x15cm"));
        assertEquals("25x32 cm", ProductoService.normalizarTamano(" 25x32 cm "));
        assertEquals("12 mm", ProductoService.normalizarTamano("12mm"));
    }

    @Test
    void tamanoVacioQuedaNull() {
        assertNull(ProductoService.normalizarTamano("  "));
        assertNull(ProductoService.normalizarTamano(null));
    }
}
