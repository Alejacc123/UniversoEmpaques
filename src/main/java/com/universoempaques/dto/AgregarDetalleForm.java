package com.universoempaques.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para agregar un producto y su cantidad a un pedido ya
 * creado (parte del flujo de RF-11).
 */
@Getter
@Setter
public class AgregarDetalleForm {

    @NotNull
    private Integer codigoPedido;

    @NotNull(message = "Debes seleccionar un producto")
    private Integer codigoProducto;

    @NotNull(message = "Debes indicar una cantidad")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
}