package com.universoempaques.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos que el area comercial ingresa para responder una
 * cotizacion solicitada por un cliente (RF-07).
 */
@Getter
@Setter
public class RegistrarCotizacionForm {

    @NotNull(message = "Debes indicar el valor de la cotizacion")
    @DecimalMin(value = "1", message = "El valor debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El valor admite maximo 10 digitos enteros y 2 decimales")
    private BigDecimal valorEstimado;

    @NotBlank(message = "Debes indicar las condiciones de la cotizacion")
    @Size(max = 1000, message = "Las condiciones no pueden superar los 1000 caracteres")
    private String condiciones;
}