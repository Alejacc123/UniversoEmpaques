package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByCliente(Cliente cliente);
    List<Pedido> findAllByOrderByFechaRegistroDesc();

    /** RF-10 / RF-15: pedidos del cliente, del mas reciente al mas antiguo. */
    List<Pedido> findByClienteOrderByFechaRegistroDesc(Cliente cliente);

    /** Detalle de un pedido solo si pertenece al cliente que lo consulta. */
    Optional<Pedido> findByCodigoAndCliente(Integer codigo, Cliente cliente);

    /** Pedido generado a partir de una cotizacion (si existe). */
    Optional<Pedido> findByCotizacion(Cotizacion cotizacion);

    boolean existsByCotizacion(Cotizacion cotizacion);
}
