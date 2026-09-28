package com.universoempaques.service;

import com.universoempaques.dto.RegistrarValorCotizacionForm;
import com.universoempaques.dto.SolicitarCotizacionForm;
import com.universoempaques.model.*;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.CotizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Logica de cotizaciones (patron Facade), RF-06 a RF-10:
 *   RF-06 el cliente solicita          -> solicitar(...)
 *   RF-07 el comercial pone el valor    -> registrarValor(...)
 *   RF-08 el cliente aprueba o rechaza  -> responder(...)
 *   RF-09 ambos consultan               -> listar... / buscar...
 *   RF-10 de la aprobada sale el pedido -> generarPedido(...)
 */
@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;

    public CotizacionService(CotizacionRepository cotizacionRepository,
                             ClienteRepository clienteRepository,
                             PedidoService pedidoService) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
    }

    // ------------------------------------------------------------------
    // Cliente
    // ------------------------------------------------------------------

    /** RF-06: el cliente solicita una cotizacion describiendo el empaque. */
    @Transactional
    public Cotizacion solicitar(SolicitarCotizacionForm form, String nitCliente) {
        Cliente cliente = buscarCliente(nitCliente);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setEspecificaciones(armarEspecificaciones(form));
        cotizacion.setEstado(EstadoCotizacionTipo.SOLICITADA);
        cotizacion.setFechaSolicitud(LocalDate.now());
        return cotizacionRepository.save(cotizacion);
    }

    /** RF-09: cotizaciones del cliente que inicio sesion. */
    public List<Cotizacion> listarDelCliente(String nitCliente) {
        return cotizacionRepository.findByClienteOrderByCodigoDesc(buscarCliente(nitCliente));
    }

    /**
     * Una cotizacion, pero SOLO si es de ese cliente. Asi un cliente no
     * puede ver la de otro cambiando el numero en la URL.
     */
    public Cotizacion buscarDelCliente(Integer codigo, String nitCliente) {
        Cotizacion cotizacion = buscarPorCodigo(codigo);
        if (cotizacion.getCliente() == null || !cotizacion.getCliente().getNit().equals(nitCliente)) {
            throw new IllegalArgumentException("La cotización no existe.");
        }
        return cotizacion;
    }

    /** RF-08: el cliente aprueba o rechaza una cotizacion que ya tiene valor. */
    @Transactional
    public Cotizacion responder(Integer codigo, String nitCliente, boolean aprobar) {
        Cotizacion cotizacion = buscarDelCliente(codigo, nitCliente);
        if (cotizacion.getEstado() != EstadoCotizacionTipo.COTIZADA) {
            throw new IllegalArgumentException("Solo puedes responder una cotización que ya tenga valor y esté pendiente.");
        }
        cotizacion.setEstado(aprobar ? EstadoCotizacionTipo.APROBADA : EstadoCotizacionTipo.RECHAZADA);
        return cotizacionRepository.save(cotizacion);
    }

    public long contarPendientesDeRespuesta(String nitCliente) {
        return cotizacionRepository.countByClienteAndEstado(buscarCliente(nitCliente), EstadoCotizacionTipo.COTIZADA);
    }

    // ------------------------------------------------------------------
    // Comercial
    // ------------------------------------------------------------------

    /** RF-09: todas, o solo las de un estado (filtro de la lista). */
    public List<Cotizacion> listar(EstadoCotizacionTipo estado) {
        return estado == null
                ? cotizacionRepository.findAllByOrderByCodigoDesc()
                : cotizacionRepository.findByEstadoOrderByCodigoAsc(estado);
    }

    public long contarPorEstado(EstadoCotizacionTipo estado) {
        return cotizacionRepository.countByEstado(estado);
    }

    /** Aprobadas por el cliente a las que el comercial aun no les genero el pedido. */
    public long contarAprobadasSinPedido() {
        return cotizacionRepository.countByEstadoAndPedidoIsNull(EstadoCotizacionTipo.APROBADA);
    }

    public Cotizacion buscarPorCodigo(Integer codigo) {
        return cotizacionRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La cotización no existe."));
    }

    /**
     * RF-07: el comercial registra el valor. Se puede corregir mientras
     * el cliente no haya respondido (SOLICITADA o COTIZADA).
     */
    @Transactional
    public Cotizacion registrarValor(Integer codigo, RegistrarValorCotizacionForm form, Usuario comercial) {
        Cotizacion cotizacion = buscarPorCodigo(codigo);
        if (cotizacion.getEstado() != EstadoCotizacionTipo.SOLICITADA
                && cotizacion.getEstado() != EstadoCotizacionTipo.COTIZADA) {
            throw new IllegalArgumentException("Esta cotización ya fue respondida por el cliente; no se puede cambiar el valor.");
        }
        cotizacion.setValor(form.getValor().doubleValue());
        cotizacion.setUsuario(comercial);
        cotizacion.setEstado(EstadoCotizacionTipo.COTIZADA);
        return cotizacionRepository.save(cotizacion);
    }

    /**
     * RF-10: a partir de una cotizacion APROBADA se crea el pedido y
     * queda enlazado (columna CodigoPedido). Solo una vez por cotizacion.
     */
    @Transactional
    public Pedido generarPedido(Integer codigo, Usuario comercial) {
        Cotizacion cotizacion = buscarPorCodigo(codigo);
        if (cotizacion.getEstado() != EstadoCotizacionTipo.APROBADA) {
            throw new IllegalArgumentException("Solo se puede generar el pedido de una cotización aprobada.");
        }
        if (cotizacion.getPedido() != null) {
            throw new IllegalArgumentException("Esta cotización ya tiene un pedido: PED-" + cotizacion.getPedido().getCodigo());
        }
        Pedido pedido = pedidoService.registrarDesdeCotizacion(cotizacion.getCliente(), comercial);
        cotizacion.setPedido(pedido);
        cotizacionRepository.save(cotizacion);
        return pedido;
    }

    /** Para mostrar en el detalle del pedido de que cotizacion salio. */
    public Optional<Cotizacion> buscarPorPedido(Pedido pedido) {
        return cotizacionRepository.findByPedido(pedido);
    }

    // ------------------------------------------------------------------

    private Cliente buscarCliente(String nit) {
        return clienteRepository.findById(nit)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
    }

    /**
     * Une los campos del formulario en un solo texto para la columna
     * VARCHAR(255). Ej: "Caja Kraft mediana | 500 und | 30x20x15 cm |
     * Cartón Kraft | Con logo a 1 tinta".
     */
    static String armarEspecificaciones(SolicitarCotizacionForm form) {
        StringBuilder texto = new StringBuilder()
                .append(form.getTipoEmpaque())
                .append(" | ").append(form.getCantidad()).append(" und")
                .append(" | ").append(form.getMedidas());
        if (form.getMaterial() != null && !form.getMaterial().isBlank()) {
            texto.append(" | ").append(form.getMaterial());
        }
        if (form.getObservaciones() != null && !form.getObservaciones().isBlank()) {
            texto.append(" | ").append(form.getObservaciones());
        }
        return texto.length() > 255 ? texto.substring(0, 255) : texto.toString();
    }
}
