package com.universoempaques.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las utilidades de DisenoService (tipo de archivo y color). */
class DisenoServiceTest {

    @Test
    void reconoceElTipoPorLaFirmaDelArchivo() {
        assertEquals("image/png", DisenoService.tipoDeContenido(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0}));
        assertEquals("image/jpeg", DisenoService.tipoDeContenido(new byte[]{(byte) 0xFF, (byte) 0xD8, 0, 0}));
        assertEquals("application/pdf", DisenoService.tipoDeContenido("%PDF-1.7".getBytes()));
        assertNull(DisenoService.tipoDeContenido("hola mundo".getBytes()));   // un .txt renombrado
        assertNull(DisenoService.tipoDeContenido(null));
    }

    @Test
    void convierteElColorAFormatoRgb() {
        assertEquals("rgb(75,147,182)", DisenoService.hexARgb("#4b93b6"));
        assertEquals("rgb(0,0,0)", DisenoService.hexARgb("#000000"));
        assertNull(DisenoService.hexARgb("azul"));
    }
}
