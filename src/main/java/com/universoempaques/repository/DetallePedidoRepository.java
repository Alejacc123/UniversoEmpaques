package com.universoempaques.repository;

import com.universoempaques.model.DetallePedido;
import com.universoempaques.model.Pedido;
import com.universoempaques.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByPedido(Pedido pedido);

    /** Cuantas lineas de pedido usan el producto (si es > 0 no se puede borrar). */
    long countByProducto(Producto producto);
}
