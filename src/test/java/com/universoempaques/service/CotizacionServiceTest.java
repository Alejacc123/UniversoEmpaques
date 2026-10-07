package com.universoempaques.service;

import com.universoempaques.dto.SolicitarCotizacionForm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba que la solicitud del cliente se guarda bien en la columna de 255 caracteres. */
class CotizacionServiceTest {

    @Test
    void armaElTextoDeEspecificaciones() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setTipoEmpaque("Caja Kraft mediana");
        form.setCantidad(500);
        form.setLargo(new BigDecimal("30")); form.setAncho(new BigDecimal("20")); form.setAlto(new BigDecimal("15"));
        form.setMaterial("Cartón Kraft");
        form.setObservaciones("Logo a una tinta");

        assertEquals("Caja Kraft mediana | 500 und | 30x20x15 cm | Cartón Kraft | Logo a una tinta",
                CotizacionService.armarEspecificaciones(form));
    }

    @Test
    void omiteLosCamposOpcionalesVacios() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setTipoEmpaque("Otro");
        form.setCantidad(10);
        form.setLargo(new BigDecimal("10")); form.setAncho(new BigDecimal("10"));

        assertEquals("Otro | 10 und | 10x10 cm", CotizacionService.armarEspecificaciones(form));
    }

    @Test
    void nuncaPasaDe255Caracteres() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setTipoEmpaque("x".repeat(40));
        form.setCantidad(1000000);
        form.setLargo(new BigDecimal("999")); form.setAncho(new BigDecimal("999")); form.setAlto(new BigDecimal("999.9"));
        form.setMaterial("z".repeat(30));
        form.setObservaciones("w".repeat(90));

        assertTrue(CotizacionService.armarEspecificaciones(form).length() <= 255);
    }

    @Test
    void lasMedidasSeArmanConNumeros() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setLargo(new BigDecimal("30.0"));
        form.setAncho(new BigDecimal("12.5"));
        assertEquals("30x12,5 cm", form.getMedidas());
        form.setAlto(new BigDecimal("8"));
        assertEquals("30x12,5x8 cm", form.getMedidas());
    }

    @Test
    void laFotoSoloPuedeSerPngOJpg() {
        assertTrue(CotizacionService.esImagen(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0}));
        assertTrue(CotizacionService.esImagen(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertFalse(CotizacionService.esImagen("hola mundo".getBytes()));
        assertFalse(CotizacionService.esImagen(new byte[]{1}));
        assertNull(CotizacionService.leerFoto(null));
    }
}
