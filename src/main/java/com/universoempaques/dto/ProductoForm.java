package com.universoempaques.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Crear o editar un producto del catalogo (Administracion > Productos).
 * El precio se escribe en pesos, SOLO con numeros: "3.500" en Colombia es
 * tres mil quinientos, pero para Java seria 3,5; por eso no se aceptan puntos.
 */
@Getter
@Setter
public class ProductoForm {

    /** null = producto nuevo. */
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "El material es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String material;

    @NotBlank(message = "El precio es obligatorio")
    @Pattern(regexp = "^[1-9]\\d{0,7}$", message = "Solo números, sin puntos ni comas (ej: 3500). Máximo 99.999.999")
    private String precio;

    @Size(max = 100, message = "Máximo 100 caracteres")
    private String forma;

    /** Ej: "30x20x15 cm", "25x32 cm" o "12 cm". Opcional. */
    @Pattern(regexp = "^$|^\\d+([.,]\\d+)?(\\s*[xX]\\s*\\d+([.,]\\d+)?){0,2}\\s*(cm|mm)$",
            message = "Usa el formato largo x ancho x alto y la unidad (ej: 30x20x15 cm o 25x32 cm)")
    private String tamano;
}
