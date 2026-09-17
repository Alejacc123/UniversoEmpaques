package com.universoempaques.service;

import com.universoempaques.dto.AgregarDetalleForm;
import com.universoempaques.dto.RegistrarPedidoForm;
import com.universoempaques.model.*;
import com.universoempaques.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    public PedidoService(PedidoRepository pedidoRepository, DetallePedidoRepository detallePedidoRepository,
                         EstadoPedidoRepository estadoPedidoRepository, ProductoRepository productoRepository,
                         ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAllByOrderByFechaRegistroDesc();
    }

    public List<Pedido> listarPorCliente(Cliente cliente) {
        return pedidoRepository.findByCliente(cliente);
    }

    public Pedido buscarPorId(Integer codigo) {
        return pedidoRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe."));
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

    /**
     * RF-11: registrar un pedido de forma directa (sin cotizacion previa).
     * Queda con estado inicial SOLICITADO (RF-14 se encarga de avanzarlo).
     */
    public Pedido registrar(RegistrarPedidoForm form, Usuario comercial) {
        Cliente cliente = clienteRepository.findById(form.getCodigoCliente())
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(comercial);
        pedido.setFechaRegistro(LocalDateTime.now());

        if (form.getFechaEntrega() != null && !form.getFechaEntrega().isBlank()) {
            pedido.setFechaEntrega(LocalDate.parse(form.getFechaEntrega()).atStartOfDay());
        }

        pedido = pedidoRepository.save(pedido);

        EstadoPedido estadoInicial = new EstadoPedido();
        estadoInicial.setPedido(pedido);
        estadoInicial.setUsuario(comercial);
        estadoInicial.setEstado(EstadoPedidoTipo.SOLICITADO);
        estadoInicial.setFechaInicio(LocalDateTime.now());
        estadoPedidoRepository.save(estadoInicial);

        return pedido;
    }

    /**
     * Agrega un producto y su cantidad al pedido (parte de RF-11).
     */
    public DetallePedido agregarDetalle(AgregarDetalleForm form) {
        Pedido pedido = buscarPorId(form.getCodigoPedido());
        Producto producto = productoRepository.findById(form.getCodigoProducto())
                .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado no existe."));

        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setProducto(producto);
        detalle.setCantidad(form.getCantidad());

        return detallePedidoRepository.save(detalle);
    }
}