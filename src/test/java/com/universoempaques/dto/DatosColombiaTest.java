package com.universoempaques.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de las reglas de validacion (no necesitan base de
 * datos ni levantar Spring). En IntelliJ: clic derecho sobre la clase
 * -> Run 'DatosColombiaTest'.
 */
class DatosColombiaTest {

    private static boolean cumple(String regla, String valor) {
        return valor.matches(regla);
    }

    @Test
    void celularesValidos() {
        assertTrue(cumple(DatosColombia.CELULAR, "3001234567"));
        assertTrue(cumple(DatosColombia.CELULAR, "300 123 4567"));
        assertTrue(cumple(DatosColombia.CELULAR, "+57 300-123-4567"));
        assertTrue(cumple(DatosColombia.CELULAR, "")); // opcional
    }

    @Test
    void celularesInvalidos() {
        assertFalse(cumple(DatosColombia.CELULAR, "12345"));
        assertFalse(cumple(DatosColombia.CELULAR, "2001234567"));   // no empieza por 3
        assertFalse(cumple(DatosColombia.CELULAR, "30012345678"));  // 11 digitos
        assertFalse(cumple(DatosColombia.CELULAR, "300abc4567"));
    }

    @Test
    void telefonoFijoNuevoFormato() {
        assertTrue(cumple(DatosColombia.TELEFONO, "6076851234"));   // Bucaramanga
        assertTrue(cumple(DatosColombia.TELEFONO, "601 234 5678")); // Bogota
        assertTrue(cumple(DatosColombia.TELEFONO, "3001234567"));   // tambien acepta celular
        assertFalse(cumple(DatosColombia.TELEFONO, "6851234"));     // formato viejo de 7 digitos
    }

    @Test
    void nit() {
        assertTrue(cumple(DatosColombia.NIT, "900123456-7"));
        assertTrue(cumple(DatosColombia.NIT, "1098765432"));
        assertFalse(cumple(DatosColombia.NIT, "900.123.456-7"));
        assertFalse(cumple(DatosColombia.NIT, "12345"));
    }

    @Test
    void contrasena() {
        assertTrue(cumple(DatosColombia.CONTRASENA, "admin123"));
        assertFalse(cumple(DatosColombia.CONTRASENA, "abcdefgh"));  // sin numeros
        assertFalse(cumple(DatosColombia.CONTRASENA, "12345678"));  // sin letras
        assertFalse(cumple(DatosColombia.CONTRASENA, "abc12"));     // muy corta
    }

    @Test
    void correo() {
        assertTrue(cumple(DatosColombia.CORREO, "ventas@universoempaques.com"));
        assertFalse(cumple(DatosColombia.CORREO, "juan@gmail"));
    }

    @Test
    void soloDigitosNormalizaElTelefono() {
        assertEquals("3001234567", DatosColombia.soloDigitos("+57 300-123-4567"));
        assertEquals("3001234567", DatosColombia.soloDigitos("300 123 4567"));
        assertNull(DatosColombia.soloDigitos("   "));
    }
}
