package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Cotizaciones (RF-06 a RF-10). */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {
    List<Cotizacion> findByClienteOrderByCodigoDesc(Cliente cliente);
    List<Cotizacion> findByEstadoOrderByCodigoAsc(EstadoCotizacionTipo estado);
    List<Cotizacion> findAllByOrderByCodigoDesc();
    Optional<Cotizacion> findByPedido(Pedido pedido);
    long countByClienteAndEstado(Cliente cliente, EstadoCotizacionTipo estado);
    long countByEstado(EstadoCotizacionTipo estado);
    List<Cotizacion> findByFechaSolicitudBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
    boolean existsByUsuario(com.universoempaques.model.Usuario usuario);
    long countByEstadoAndPedidoIsNull(EstadoCotizacionTipo estado);
}
