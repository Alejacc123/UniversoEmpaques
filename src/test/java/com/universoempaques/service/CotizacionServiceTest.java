package com.universoempaques.service;

import com.universoempaques.dto.SolicitarCotizacionForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba que la solicitud del cliente se guarda bien en la columna de 255 caracteres. */
class CotizacionServiceTest {

    @Test
    void armaElTextoDeEspecificaciones() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setTipoEmpaque("Caja Kraft mediana");
        form.setCantidad(500);
        form.setMedidas("30x20x15 cm");
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
        form.setMedidas("10x10 cm");

        assertEquals("Otro | 10 und | 10x10 cm", CotizacionService.armarEspecificaciones(form));
    }

    @Test
    void nuncaPasaDe255Caracteres() {
        SolicitarCotizacionForm form = new SolicitarCotizacionForm();
        form.setTipoEmpaque("x".repeat(40));
        form.setCantidad(1000000);
        form.setMedidas("y".repeat(30));
        form.setMaterial("z".repeat(30));
        form.setObservaciones("w".repeat(90));

        assertTrue(CotizacionService.armarEspecificaciones(form).length() <= 255);
    }
}
