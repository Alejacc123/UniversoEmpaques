package com.universoempaques.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para registrar un pedido de forma directa (RF-11), es decir,
 * sin pasar por una cotizacion previa (ese flujo se agregara cuando
 * exista la tabla de Cotizacion).
 */
@Getter
@Setter
public class RegistrarPedidoForm {

    @NotNull(message = "Debes seleccionar un cliente")
    private Integer codigoCliente;

    // Fecha de entrega estimada, en formato yyyy-MM-dd (input type="date")
    private String fechaEntrega;
}