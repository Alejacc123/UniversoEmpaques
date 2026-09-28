package com.universoempaques.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Valor que el area COMERCIAL le pone a una cotizacion (RF-07). */
@Getter
@Setter
public class RegistrarValorCotizacionForm {

    @NotNull(message = "Indica el valor de la cotización")
    @DecimalMin(value = "1", message = "El valor debe ser mayor a 0")
    @DecimalMax(value = "9999999999", message = "Valor demasiado alto")
    @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales")
    private BigDecimal valor;
}
