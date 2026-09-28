package com.universoempaques.service;

import com.universoempaques.dto.AgregarDetalleForm;
import com.universoempaques.dto.RegistrarPedidoForm;
import com.universoempaques.dto.SolicitarPedidoForm;
import com.universoempaques.model.*;
import com.universoempaques.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica de pedidos (RF-11 por ahora; luego RF-10, RF-14, RF-15).
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final DisenoRepository disenoRepository;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidoRepository, DetallePedidoRepository detallePedidoRepository,
                         EstadoPedidoRepository estadoPedidoRepository, ProductoRepository productoRepository,
                         ClienteRepository clienteRepository, DisenoRepository disenoRepository,
                         ApplicationEventPublisher eventos) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.disenoRepository = disenoRepository;
        this.eventos = eventos;
    }

    /** Solo los clientes activos pueden recibir pedidos nuevos. */
    public List<Cliente> listarClientesActivos() {
        return clienteRepository.findAllByOrderByNombreAsc().stream()
                .filter(Cliente::estaActivo)
                .toList();
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

    public long contarPorCliente(String nit) {
        return clienteRepository.findById(nit).map(c -> (long) pedidoRepository.findByCliente(c).size()).orElse(0L);
    }

    public Pedido buscarPorId(Integer codigo) {
        return pedidoRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe."));
    }

    public List<DetallePedido> listarDetalles(Pedido pedido) {
        return detallePedidoRepository.findByPedido(pedido);
    }

    /** Suma de los subtotales (cantidad x precio unitario guardado). */
    public BigDecimal calcularTotal(List<DetallePedido> detalles) {
        return detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public EstadoPedido estadoActual(Pedido pedido) {
        return estadoPedidoRepository.findTopByPedidoOrderByCodigoDesc(pedido);
    }

    public List<EstadoPedido> historialEstados(Pedido pedido) {
        return estadoPedidoRepository.findByPedidoOrderByCodigoAsc(pedido);
    }

    /**
     * RF-11: registrar un pedido directo (sin cotizacion previa).
     * Queda con estado inicial SOLICITADO en la tabla EstadoPedido.
     */
    @Transactional
    public Pedido registrar(RegistrarPedidoForm form, Usuario comercial) {
        Cliente cliente = clienteRepository.findById(form.getNitCliente())
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        if (!cliente.estaActivo()) {
            throw new IllegalArgumentException("El cliente esta inactivo; no se le pueden registrar pedidos.");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(comercial);
        pedido.setFechaRegistro(LocalDateTime.now());
        pedido.setFormaPago(vacioANull(form.getFormaPago()));

        String direccion = vacioANull(form.getDireccionEntrega());
        pedido.setDireccionEntrega(direccion != null ? direccion : cliente.getDireccion());

        if (form.getFechaEntrega() != null && !form.getFechaEntrega().isBlank()) {
            LocalDate fechaEntrega = LocalDate.parse(form.getFechaEntrega());
            if (fechaEntrega.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de entrega no puede ser anterior a hoy.");
            }
            pedido.setFechaEntrega(fechaEntrega.atStartOfDay());
        }

        pedido = pedidoRepository.save(pedido);
        registrarEstadoInicial(pedido, comercial);
        return pedido;
    }

    /**
     * RF-10: el CLIENTE solicita un pedido directo desde la plataforma.
     * Queda SOLICITADO y sin comercial asignado (CodigoUsuario = NULL):
     * el comercial que lo envie a diseno queda como responsable.
     * El precio de cada producto se copia en ese momento (PrecioUnitario).
     */
    @Transactional
    public Pedido solicitarPorCliente(SolicitarPedidoForm form, String nit) {
        Cliente cliente = clienteRepository.findById(nit)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
        var elegidos = form.getCantidades().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && e.getValue() > 0)
                .toList();
        if (elegidos.isEmpty()) {
            throw new IllegalArgumentException("Indica la cantidad de al menos un producto.");
        }
        if (elegidos.stream().anyMatch(e -> e.getValue() > 1_000_000)) {
            throw new IllegalArgumentException("La cantidad máxima por producto es 1.000.000.");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(null);
        pedido.setFechaRegistro(LocalDateTime.now());
        pedido.setFormaPago(vacioANull(form.getFormaPago()));
        String direccion = vacioANull(form.getDireccionEntrega());
        pedido.setDireccionEntrega(direccion != null ? direccion : cliente.getDireccion());
        if (form.getFechaEntrega() != null && !form.getFechaEntrega().isBlank()) {
            LocalDate fecha = LocalDate.parse(form.getFechaEntrega());
            if (fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de entrega no puede ser anterior a hoy.");
            }
            pedido.setFechaEntrega(fecha.atStartOfDay());
        }
        pedido = pedidoRepository.save(pedido);

        for (var e : elegidos) {
            Producto producto = productoRepository.findById(e.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no existe."));
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(e.getValue());
            detalle.setPrecioUnitario(producto.getPrecio());
            detallePedidoRepository.save(detalle);
        }
        registrarEstadoInicial(pedido, null);
        return pedido;
    }

    /**
     * RF-10: pedido que nace de una cotizacion APROBADA (lo llama
     * CotizacionService.generarPedido). Usa la direccion del cliente;
     * los productos se agregan despues desde el detalle del pedido.
     */
    @Transactional
    public Pedido registrarDesdeCotizacion(Cliente cliente, Usuario comercial) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(comercial);
        pedido.setFechaRegistro(LocalDateTime.now());
        pedido.setDireccionEntrega(cliente.getDireccion());
        pedido = pedidoRepository.save(pedido);
        registrarEstadoInicial(pedido, comercial);
        return pedido;
    }

    /** Todo pedido nuevo arranca en SOLICITADO (tabla EstadoPedido). */
    private void registrarEstadoInicial(Pedido pedido, Usuario usuario) {
        EstadoPedido estadoInicial = new EstadoPedido();
        estadoInicial.setPedido(pedido);
        estadoInicial.setUsuario(usuario);
        estadoInicial.setEstado(EstadoPedidoTipo.SOLICITADO);
        estadoInicial.setFechaInicio(LocalDateTime.now());
        estadoPedidoRepository.save(estadoInicial);
        eventos.publishEvent(new EstadoPedidoCambiadoEvent(pedido.getCodigo(), EstadoPedidoTipo.SOLICITADO, null));
    }

    // ------------------------------------------------------------------
    // RF-14 / RF-17: avanzar el pedido por sus etapas
    // ------------------------------------------------------------------

    /**
     * ¿Puede este usuario hacer avanzar el pedido desde su estado actual?
     * El Administrador siempre; los demas, solo si su area es la
     * responsable de ese estado (ver EstadoPedidoTipo).
     */
    public boolean puedeAvanzar(Pedido pedido, Usuario usuario) {
        EstadoPedido actual = estadoActual(pedido);
        if (usuario == null || actual == null || actual.getEstado().esFinal()) {
            return false;
        }
        if (usuario.esAdministrador()) {
            return true;
        }
        Area area = usuario.getAreaPrincipal();
        return area != null && area.getNombre() != null
                && area.getNombre().equalsIgnoreCase(actual.getEstado().getAreaResponsable());
    }

    /**
     * Pasa el pedido al siguiente estado: cierra la etapa actual
     * (FechaFin = ahora) y abre la nueva con quien la registro y su
     * observacion. Luego avisa (evento) para la notificacion al cliente.
     */
    @Transactional
    public EstadoPedido avanzarEstado(Integer codigoPedido, Usuario usuario, String reporte) {
        Pedido pedido = buscarPorId(codigoPedido);
        EstadoPedido actual = estadoActual(pedido);
        if (actual == null || actual.getEstado().esFinal()) {
            throw new IllegalArgumentException("Este pedido ya fue entregado; no tiene más etapas.");
        }
        if (!puedeAvanzar(pedido, usuario)) {
            throw new IllegalArgumentException("Tu área no es la responsable de la etapa \""
                    + actual.getEstado().getEtiqueta() + "\".");
        }
        if (actual.getEstado() == EstadoPedidoTipo.SOLICITADO && listarDetalles(pedido).isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto antes de enviar el pedido a diseño.");
        }
        // RF-13: a produccion solo pasa si el diseno de TODOS los productos esta aprobado
        if (actual.getEstado() == EstadoPedidoTipo.EN_DISENO) {
            boolean todosAprobados = listarDetalles(pedido).stream().allMatch(d ->
                    disenoRepository.findTopByDetallePedidoOrderByVersionDesc(d)
                            .map(dis -> dis.getEstado() == EstadoDisenoTipo.APROBADO).orElse(false));
            if (!todosAprobados) {
                throw new IllegalArgumentException("Todos los productos deben tener su diseño aprobado antes de pasar a producción.");
            }
        }
        String nota = vacioANull(reporte);
        if (nota != null && nota.length() > 255) {
            throw new IllegalArgumentException("La observación puede tener máximo 255 caracteres.");
        }

        if (pedido.getUsuario() == null && usuario != null) {
            pedido.setUsuario(usuario);   // pedido hecho por el cliente: queda a cargo de quien lo gestiona
            pedidoRepository.save(pedido);
        }

        LocalDateTime ahora = LocalDateTime.now();
        actual.setFechaFin(ahora);
        estadoPedidoRepository.save(actual);

        EstadoPedido nuevo = new EstadoPedido();
        nuevo.setPedido(pedido);
        nuevo.setUsuario(usuario);
        nuevo.setEstado(actual.getEstado().siguiente());
        nuevo.setFechaInicio(ahora);
        nuevo.setReporte(nota);
        // Al entregar, la etapa final queda cerrada de una vez
        if (nuevo.getEstado().esFinal()) {
            nuevo.setFechaFin(ahora);
        }
        estadoPedidoRepository.save(nuevo);

        eventos.publishEvent(new EstadoPedidoCambiadoEvent(pedido.getCodigo(), nuevo.getEstado(), nota));
        return nuevo;
    }

    /** Pedidos cuyo estado ACTUAL es alguno de estos (cola de trabajo de un area). */
    public List<EstadoPedido> pedidosEnEstados(java.util.Collection<EstadoPedidoTipo> estados) {
        return estadoPedidoRepository.findByFechaFinIsNullAndEstadoInOrderByCodigoAsc(estados);
    }

    public long contarEnEstados(java.util.Collection<EstadoPedidoTipo> estados) {
        return estadoPedidoRepository.countByFechaFinIsNullAndEstadoIn(estados);
    }

    /** RF-15: pedidos del cliente, del mas nuevo al mas viejo. */
    public List<Pedido> listarDelCliente(String nit) {
        return clienteRepository.findById(nit)
                .map(pedidoRepository::findByClienteOrderByCodigoDesc)
                .orElse(List.of());
    }

    /** Un pedido, pero SOLO si es de ese cliente (no se puede ver el de otro cambiando la URL). */
    public Pedido buscarDelCliente(Integer codigo, String nit) {
        Pedido pedido = buscarPorId(codigo);
        if (pedido.getCliente() == null || !pedido.getCliente().getNit().equals(nit)) {
            throw new IllegalArgumentException("El pedido no existe.");
        }
        return pedido;
    }

    /**
     * Agrega un producto al pedido (parte de RF-11). Copia el precio
     * actual del producto a PrecioUnitario: si manana cambia el precio
     * de lista, este pedido conserva el precio con el que se vendio.
     */
    /**
     * Los productos del pedido solo se pueden cambiar mientras el pedido
     * este en SOLICITADO (antes de pasar a diseno/produccion).
     */
    public boolean esEditable(Pedido pedido) {
        EstadoPedido actual = estadoActual(pedido);
        return actual == null || actual.getEstado() == EstadoPedidoTipo.SOLICITADO;
    }

    @Transactional
    public DetallePedido agregarDetalle(AgregarDetalleForm form) {
        Pedido pedido = buscarPorId(form.getCodigoPedido());
        if (!esEditable(pedido)) {
            throw new IllegalArgumentException("El pedido ya avanzó de etapa; no se le pueden agregar productos.");
        }
        Producto producto = productoRepository.findById(form.getCodigoProducto())
                .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado no existe."));

        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setProducto(producto);
        detalle.setCantidad(form.getCantidad());
        detalle.setPrecioUnitario(producto.getPrecio());

        return detallePedidoRepository.save(detalle);
    }

    /** Quita un producto del pedido (solo si sigue en SOLICITADO y no tiene disenos). */
    @Transactional
    public void quitarDetalle(Integer codigoPedido, Integer codigoDetalle) {
        Pedido pedido = buscarPorId(codigoPedido);
        DetallePedido detalle = detallePedidoRepository.findById(codigoDetalle)
                .filter(d -> d.getPedido().getCodigo().equals(codigoPedido))
                .orElseThrow(() -> new IllegalArgumentException("Ese producto no pertenece al pedido."));
        if (!esEditable(pedido)) {
            throw new IllegalArgumentException("El pedido ya avanzó de etapa; no se le pueden quitar productos.");
        }
        if (!disenoRepository.findByDetallePedidoOrderByVersionDesc(detalle).isEmpty()) {
            throw new IllegalArgumentException("Ese producto ya tiene diseños asociados; no se puede quitar.");
        }
        detallePedidoRepository.delete(detalle);
    }

    private static String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
