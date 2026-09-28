package com.universoempaques.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Pedido DIRECTO que hace el cliente desde la plataforma (RF-10), sin
 * pasar por una cotizacion: elige productos del catalogo y cantidades.
 *
 * "cantidades" llega del formulario como cantidades[codigoProducto] = N;
 * los productos que deja en 0 o vacios no se incluyen.
 * Que haya al menos un producto y la fecha no sea pasada lo revisa PedidoService.
 */
@Getter
@Setter
public class SolicitarPedidoForm {

    private Map<Integer, Integer> cantidades = new HashMap<>();

    /** Fecha deseada de entrega, yyyy-MM-dd (input type="date"). Opcional. */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Fecha inválida")
    private String fechaEntrega;

    @Size(max = 50, message = "Máximo 50 caracteres")
    private String formaPago;

    /** Vacia = la direccion registrada del cliente. */
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccionEntrega;
}
