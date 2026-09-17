package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByCliente(Cliente cliente);
    List<Pedido> findAllByOrderByFechaRegistroDesc();
}
