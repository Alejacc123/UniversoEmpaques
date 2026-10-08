package com.universoempaques.dto;

/**
 * Reglas de validacion para datos colombianos, en UN solo lugar.
 * Los DTOs las usan en @Pattern(regexp = DatosColombia.XXX) y los
 * services usan soloDigitos(...) para guardar los numeros "limpios".
 *
 * Asi, si cambia una regla (ej. el formato del NIT), se cambia aqui
 * y aplica en todos los formularios.
 */
public final class DatosColombia {

    private DatosColombia() {
    }

    /**
     * Celular colombiano: 10 digitos que empiezan por 3.
     * Acepta que el usuario escriba +57, espacios o guiones:
     * "3001234567", "300 123 4567", "+57 300-123-4567".
     */
    public static final String CELULAR =
            "^$|^(\\+?57[ -]?)?3\\d{2}[ -]?\\d{3}[ -]?\\d{4}$";

    /**
     * Telefono fijo o celular. Desde 2021 los fijos en Colombia tambien
     * tienen 10 digitos: 60 + indicativo + 7 digitos (ej: 607 6851234
     * en Bucaramanga). Tambien acepta celulares.
     */
    public static final String TELEFONO =
            "^$|^(\\+?57[ -]?)?(3\\d{2}|60[1-8])[ -]?\\d{3}[ -]?\\d{4}$";

    /** NIT o cedula: 6 a 10 digitos, con o sin digito de verificacion (900123456-7). */
    public static final String NIT = "^\\d{6,10}(-\\d)?$";

    /** Cedula de ciudadania: exactamente 10 digitos (pedido de la Product Owner). */
    public static final String DOCUMENTO = "^$|^\\d{10}$";

    /** Nombre de persona: letras (con tildes y n), espacios, punto, apostrofe y guion. */
    public static final String NOMBRE_PERSONA = "^[\\p{L} .'-]{2,100}$";

    /** Nombre de empresa: ademas permite numeros y & , ( ). */
    public static final String NOMBRE_EMPRESA = "^$|^[\\p{L}0-9 .,&'()-]{2,150}$";

    /** Correo con dominio real (exige el punto: nombre@dominio.com). */
    public static final String CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /** Contrasena: 8 a 64 caracteres, con al menos una letra y un numero. */
    public static final String CONTRASENA = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    /** Igual que CONTRASENA, pero puede ir vacia (editar sin cambiarla). */
    public static final String CONTRASENA_OPCIONAL = "^$|^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    public static final String MSG_CELULAR = "Celular inválido: 10 dígitos que empiezan por 3 (ej: 3001234567)";
    public static final String MSG_TELEFONO = "Teléfono inválido: 10 dígitos, celular (3xx) o fijo (60x) (ej: 6076851234)";
    public static final String MSG_NIT = "NIT inválido: 6 a 10 dígitos, con o sin dígito de verificación (ej: 900123456-7)";
    public static final String MSG_DOCUMENTO = "Documento inválido: deben ser exactamente 10 números";
    public static final String MSG_NOMBRE_PERSONA = "Nombre inválido: solo letras y espacios (2 a 100 caracteres)";
    public static final String MSG_NOMBRE_EMPRESA = "Nombre inválido: letras, números y . , & ( ) - (2 a 150 caracteres)";
    public static final String MSG_CORREO = "Correo inválido (ej: nombre@empresa.com)";
    public static final String MSG_CONTRASENA = "Mínimo 8 caracteres, con al menos una letra y un número";

    /**
     * Deja solo los digitos de un telefono para guardarlo siempre igual:
     * "+57 300-123-4567" -> "3001234567". Vacio -> null.
     */
    public static String soloDigitos(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("57")) {
            digitos = digitos.substring(2); // quita el indicativo de pais
        }
        return digitos;
    }
}
