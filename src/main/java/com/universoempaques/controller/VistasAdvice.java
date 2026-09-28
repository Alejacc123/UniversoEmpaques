package com.universoempaques.controller;

import com.universoempaques.model.EstadoPedidoTipo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Datos que TODAS las vistas pueden usar. Aqui: la lista de estados de
 * un pedido, para dibujar la barra de progreso (fragments/layout :: progreso).
 */
@ControllerAdvice
public class VistasAdvice {

    @ModelAttribute("estadosPedido")
    public EstadoPedidoTipo[] estadosPedido() {
        return EstadoPedidoTipo.values();
    }
}
