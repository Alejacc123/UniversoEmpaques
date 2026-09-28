package com.universoempaques.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Datos que el cliente envia al solicitar un pedido (RF-10).
 *
 * - cantidades: solo aplica al pedido DIRECTO. La llave es el codigo
 *   del producto y el valor la cantidad (0 o vacio = no lo incluye).
 * - especificacionesDiseno: opcional en ambos flujos. Si viene con
 *   texto, se crea un Diseno PENDIENTE para el area de Diseno
 *   (ver diagrama de secuencia 4.1, bloque opt).
 */
@Getter
@Setter
public class SolicitarPedidoForm {

    private Map<Integer, Integer> cantidades = new HashMap<>();

    @Size(max = 255, message = "Las especificaciones del diseno no pueden superar los 255 caracteres")
    private String especificacionesDiseno;
}
