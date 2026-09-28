package com.universoempaques.repository;

import com.universoempaques.model.DetallePedido;
import com.universoempaques.model.Diseno;
import com.universoempaques.model.EstadoDisenoTipo;
import com.universoempaques.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * En el modelo v2 el Diseno cuelga de DetallePedido (ya no de Pedido):
 * cada producto del pedido tiene su diseno, con varias versiones.
 */
public interface DisenoRepository extends JpaRepository<Diseno, Integer> {
    List<Diseno> findByDetallePedidoOrderByVersionDesc(DetallePedido detallePedido);
    Optional<Diseno> findTopByDetallePedidoOrderByVersionDesc(DetallePedido detallePedido);
    List<Diseno> findByEstado(EstadoDisenoTipo estado);
    boolean existsByUsuario(Usuario usuario);
}
