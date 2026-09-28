package com.universoempaques.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos que el cliente indica al solicitar una cotizacion (RF-06).
 * Las medidas se piden por separado (en cm) para validarlas como
 * numeros; el servicio las une en un solo texto, ej: "30 x 20 x 15 cm".
 */
@Getter
@Setter
public class SolicitarCotizacionForm {

    @NotBlank(message = "Debes indicar el material")
    @Size(max = 255, message = "El material no puede superar los 255 caracteres")
    private String material;

    @NotNull(message = "Debes indicar el largo")
    @DecimalMin(value = "0.1", message = "El largo debe ser mayor a cero")
    @Digits(integer = 5, fraction = 1, message = "Usa maximo un decimal")
    private BigDecimal largo;

    @NotNull(message = "Debes indicar el ancho")
    @DecimalMin(value = "0.1", message = "El ancho debe ser mayor a cero")
    @Digits(integer = 5, fraction = 1, message = "Usa maximo un decimal")
    private BigDecimal ancho;

    /** Opcional: las bolsas o laminas no tienen alto. */
    @DecimalMin(value = "0.1", message = "El alto debe ser mayor a cero")
    @Digits(integer = 5, fraction = 1, message = "Usa maximo un decimal")
    private BigDecimal alto;

    @NotNull(message = "Debes indicar la cantidad")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 1000000, message = "La cantidad no puede superar 1.000.000 unidades")
    private Integer cantidad;

    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    private String descripcion;
}
