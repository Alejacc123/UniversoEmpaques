package com.universoempaques.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para registrar un pedido de forma directa (RF-11), sin
 * cotizacion previa. El cliente se identifica por su NIT.
 * Que la fecha de entrega no sea pasada lo revisa PedidoService.
 */
@Getter
@Setter
public class RegistrarPedidoForm {

    @NotBlank(message = "Debes seleccionar un cliente")
    private String nitCliente;

    /** Fecha de entrega estimada, formato yyyy-MM-dd (input type="date"). */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Fecha inválida")
    private String fechaEntrega;

    @Size(max = 50, message = "Máximo 50 caracteres")
    private String formaPago;

    /** Si se deja vacia, se usa la direccion registrada del cliente. */
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccionEntrega;
}
