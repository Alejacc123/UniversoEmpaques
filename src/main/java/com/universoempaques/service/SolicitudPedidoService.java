package com.universoempaques.service;

import com.universoempaques.dto.SolicitarPedidoForm;
import com.universoempaques.model.*;
import com.universoempaques.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RF-10: el cliente solicita un pedido, ya sea a partir de una
 * cotizacion aprobada o de forma directa desde el catalogo.
 */
@Service
public class SolicitudPedidoService {

    private static final int CANTIDAD_MAXIMA = 1_000_000;

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final DisenoRepository disenoRepository;

    public SolicitudPedidoService(PedidoRepository pedidoRepository,
                                  DetallePedidoRepository detallePedidoRepository,
                                  EstadoPedidoRepository estadoPedidoRepository,
                                  ProductoRepository productoRepository,
                                  ClienteRepository clienteRepository,
                                  CotizacionRepository cotizacionRepository,
                                  DisenoRepository disenoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.disenoRepository = disenoRepository;
    }

    // ------------------------------------------------------------------
    // Solicitar pedido (RF-10)
    // ------------------------------------------------------------------

    /** Pedido directo: el cliente elige productos del catalogo y sus cantidades. */
    @Transactional
    public Pedido solicitarDirecto(SolicitarPedidoForm form, Cliente clienteSesion) {
        Cliente cliente = recargarCliente(clienteSesion);
        List<DetallePedido> detalles = construirDetalles(form.getCantidades());

        Pedido pedido = crearPedido(cliente, null);
        for (DetallePedido detalle : detalles) {
            detalle.setPedido(pedido);
        }
        detallePedidoRepository.saveAll(detalles);

        registrarDisenoSiAplica(pedido, form.getEspecificacionesDiseno());
        registrarEstadoInicial(pedido, "Pedido directo solicitado por el cliente.");
        return pedido;
    }

    /** Pedido a partir de una cotizacion APROBADA del mismo cliente. */
    @Transactional
    public Pedido solicitarDesdeCotizacion(Integer codigoCotizacion, SolicitarPedidoForm form,
                                           Cliente clienteSesion) {
        Cliente cliente = recargarCliente(clienteSesion);
        Cotizacion cotizacion = cotizacionRepository.findByCodigoAndCliente(codigoCotizacion, cliente)
                .orElseThrow(() -> new IllegalArgumentException("La cotizacion no existe."));

        if (cotizacion.getEstado() != EstadoCotizacionTipo.APROBADA) {
            throw new IllegalStateException("Solo puedes solicitar un pedido a partir de una cotizacion aprobada.");
        }
        if (pedidoRepository.existsByCotizacion(cotizacion)) {
            throw new IllegalStateException("Esta cotizacion ya tiene un pedido asociado.");
        }

        Pedido pedido = crearPedido(cliente, cotizacion);
        registrarDisenoSiAplica(pedido, form.getEspecificacionesDiseno());
        registrarEstadoInicial(pedido, "Pedido solicitado a partir de la cotizacion COT-" + cotizacion.getCodigo() + ".");
        return pedido;
    }

    // ------------------------------------------------------------------
    // Consultas del cliente
    // ------------------------------------------------------------------

    public List<Producto> listarCatalogo() {
        return productoRepository.findAll(Sort.by("nombre"));
    }

    public List<Pedido> listarDeCliente(Cliente cliente) {
        return pedidoRepository.findByClienteOrderByFechaRegistroDesc(cliente);
    }

    public Optional<Pedido> buscarDeCliente(Integer codigo, Cliente cliente) {
        return pedidoRepository.findByCodigoAndCliente(codigo, cliente);
    }

    public Optional<Pedido> pedidoDeCotizacion(Cotizacion cotizacion) {
        return pedidoRepository.findByCotizacion(cotizacion);
    }

    public List<DetallePedido> listarDetalles(Pedido pedido) {
        return detallePedidoRepository.findByPedido(pedido);
    }

    public EstadoPedido estadoActual(Pedido pedido) {
        return estadoPedidoRepository.findTopByPedidoOrderByFechaInicioDesc(pedido);
    }

    public List<EstadoPedido> historialEstados(Pedido pedido) {
        return estadoPedidoRepository.findByPedidoOrderByFechaInicioAsc(pedido);
    }

    public Optional<Diseno> disenoDe(Pedido pedido) {
        return disenoRepository.findByPedido(pedido);
    }

    /** Estado actual de cada pedido, indexado por codigo (para las tablas). */
    public Map<Integer, EstadoPedido> estadosActuales(List<Pedido> pedidos) {
        Map<Integer, EstadoPedido> estados = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            estados.put(pedido.getCodigo(), estadoActual(pedido));
        }
        return estados;
    }

    /**
     * Valor estimado del pedido: el de la cotizacion si viene de una,
     * o la suma de precio x cantidad de sus productos si es directo.
     */
    public BigDecimal totalEstimado(Pedido pedido, List<DetallePedido> detalles) {
        if (pedido.getCotizacion() != null) {
            return pedido.getCotizacion().getValorEstimado();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido detalle : detalles) {
            BigDecimal precio = detalle.getProducto().getPrecio();
            if (precio != null && detalle.getCantidad() != null) {
                total = total.add(precio.multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }
        }
        return total;
    }

    public long contarActivos(Cliente cliente) {
        return listarDeCliente(cliente).stream()
                .filter(p -> !estaEntregado(p))
                .count();
    }

    public long contarEntregados(Cliente cliente) {
        return listarDeCliente(cliente).stream()
                .filter(this::estaEntregado)
                .count();
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private boolean estaEntregado(Pedido pedido) {
        EstadoPedido estado = estadoActual(pedido);
        return estado != null && estado.getEstado() == EstadoPedidoTipo.ENTREGADO;
    }

    private Cliente recargarCliente(Cliente clienteSesion) {
        return clienteRepository.findById(clienteSesion.getNit())
                .orElseThrow(() -> new IllegalArgumentException("Tu cuenta de cliente no existe."));
    }

    private List<DetallePedido> construirDetalles(Map<Integer, Integer> cantidades) {
        List<DetallePedido> detalles = new ArrayList<>();
        if (cantidades != null) {
            for (Map.Entry<Integer, Integer> linea : cantidades.entrySet()) {
                Integer cantidad = linea.getValue();
                if (linea.getKey() == null || cantidad == null || cantidad == 0) {
                    continue;
                }
                if (cantidad < 0 || cantidad > CANTIDAD_MAXIMA) {
                    throw new IllegalArgumentException("Las cantidades deben estar entre 1 y 1.000.000 unidades.");
                }
                Producto producto = productoRepository.findById(linea.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("Uno de los productos seleccionados ya no existe."));

                DetallePedido detalle = new DetallePedido();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                detalles.add(detalle);
            }
        }
        if (detalles.isEmpty()) {
            throw new IllegalArgumentException("Indica la cantidad de al menos un producto.");
        }
        return detalles;
    }

    private Pedido crearPedido(Cliente cliente, Cotizacion cotizacion) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setCotizacion(cotizacion);
        pedido.setFechaRegistro(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    /** Diagrama de secuencia 4.1 (opt): si el pedido incluye diseno personalizado. */
    private void registrarDisenoSiAplica(Pedido pedido, String especificaciones) {
        if (especificaciones == null || especificaciones.isBlank()) {
            return;
        }
        Diseno diseno = new Diseno();
        diseno.setPedido(pedido);
        diseno.setEspecificacionesTecnicas(especificaciones.trim());
        diseno.setEstado(EstadoDisenoTipo.PENDIENTE);
        disenoRepository.save(diseno);
    }

    /** El usuario queda en null porque quien solicita es el cliente, no un trabajador. */
    private void registrarEstadoInicial(Pedido pedido, String reporte) {
        EstadoPedido estado = new EstadoPedido();
        estado.setPedido(pedido);
        estado.setEstado(EstadoPedidoTipo.SOLICITADO);
        estado.setFechaInicio(LocalDateTime.now());
        estado.setReporte(reporte);
        estadoPedidoRepository.save(estado);
    }
}