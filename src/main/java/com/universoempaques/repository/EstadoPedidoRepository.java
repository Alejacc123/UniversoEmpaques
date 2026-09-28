package com.universoempaques.repository;

import com.universoempaques.model.EstadoPedido;
import com.universoempaques.model.EstadoPedidoTipo;
import com.universoempaques.model.Pedido;
import com.universoempaques.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Historial de estados. Se ordena por Codigo (autoincremental) y no por
 * fecha: la columna DATETIME guarda solo segundos, y dos cambios en el
 * mismo segundo quedarian empatados.
 */
public interface EstadoPedidoRepository extends JpaRepository<EstadoPedido, Integer> {
    List<EstadoPedido> findByPedidoOrderByCodigoAsc(Pedido pedido);
    EstadoPedido findTopByPedidoOrderByCodigoDesc(Pedido pedido);
    boolean existsByUsuario(Usuario usuario);

    /**
     * Estado ACTUAL de cada pedido = la fila sin FechaFin (al avanzar se
     * cierra la anterior). Sirve para la "cola de trabajo" de cada area.
     */
    List<EstadoPedido> findByFechaFinIsNullAndEstadoInOrderByCodigoAsc(Collection<EstadoPedidoTipo> estados);
    long countByFechaFinIsNullAndEstadoIn(Collection<EstadoPedidoTipo> estados);
}
