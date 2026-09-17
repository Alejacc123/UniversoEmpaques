package com.universoempaques.repository;

import com.universoempaques.model.EstadoPedido;
import com.universoempaques.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadoPedidoRepository extends JpaRepository<EstadoPedido, Integer> {
    List<EstadoPedido> findByPedidoOrderByFechaInicioAsc(Pedido pedido);
    EstadoPedido findTopByPedidoOrderByFechaInicioDesc(Pedido pedido);
}
