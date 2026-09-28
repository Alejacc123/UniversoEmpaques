package com.universoempaques;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoCotizacionTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    /** RF-09: cotizaciones de un cliente, de la mas reciente a la mas antigua. */
    List<Cotizacion> findByClienteOrderByFechaSolicitudDesc(Cliente cliente);

    /** RF-09: detalle de una cotizacion solo si pertenece al cliente que la consulta. */
    Optional<Cotizacion> findByCodigoAndCliente(Integer codigo, Cliente cliente);

    /** RF-07: bandeja del area comercial (ej: las SOLICITADAS pendientes de registrar). */
    List<Cotizacion> findByEstadoOrderByFechaSolicitudAsc(EstadoCotizacionTipo estado);

    /** Listado general para el area comercial y el administrador. */
    List<Cotizacion> findAllByOrderByFechaSolicitudDesc();

    /** Contador del panel del cliente (ej: "Cotizaciones pendientes"). */
    long countByClienteAndEstado(Cliente cliente, EstadoCotizacionTipo estado);

    /** RF-18: conteos por estado para reportes. */
    long countByEstado(EstadoCotizacionTipo estado);
}
