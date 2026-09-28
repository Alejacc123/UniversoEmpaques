package com.universoempaques.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prueba el formato de los tiempos del reporte (RF-18). */
class ReporteServiceTest {

    @Test
    void muestraLosTiemposDeFormaLegible() {
        assertEquals("2 d 4 h", ReporteService.legible(Duration.ofHours(52)));
        assertEquals("1 h 1 min", ReporteService.legible(Duration.ofSeconds(3700)));
        assertEquals("15 min", ReporteService.legible(Duration.ofMinutes(15)));
        assertEquals("1 min", ReporteService.legible(Duration.ofSeconds(20)));
        assertEquals("0 min", ReporteService.legible(Duration.ZERO));
    }
}
