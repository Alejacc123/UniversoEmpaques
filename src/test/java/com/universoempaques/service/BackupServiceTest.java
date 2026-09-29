package com.universoempaques.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-19: que archivos se pueden restaurar desde la pantalla del admin.
 * (La restauracion en si necesita MySQL, por eso aqui solo se prueba la regla.)
 */
class BackupServiceTest {

    @Test
    void seRestauranLasCopiasCompletasYLosParciales() {
        assertTrue(BackupService.esRestaurable("mensual.sql"));
        assertTrue(BackupService.esRestaurable("semanal.sql"));
        assertTrue(BackupService.esRestaurable("parcial_3-miercoles.sql"));
        assertTrue(BackupService.esRestaurable(BackupService.ANTES_DE_RESTAURAR));
    }

    @Test
    void noSeRestauranOtrosArchivosNiRutasRaras() {
        assertFalse(BackupService.esRestaurable(null));
        assertFalse(BackupService.esRestaurable("otro.sql"));
        assertFalse(BackupService.esRestaurable("parcial_.sql"));
        assertFalse(BackupService.esRestaurable("../mensual.sql"));
        assertFalse(BackupService.esRestaurable("configuracion.properties"));
    }
}
