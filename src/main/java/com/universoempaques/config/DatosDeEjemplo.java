package com.universoempaques.config;

import com.universoempaques.model.*;
import com.universoempaques.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de ejemplo para probar y mostrar el sistema sin tener que crear
 * todo a mano. Los llama DataSeeder cuando app.datos-prueba=true y SOLO
 * si la base no tiene pedidos ni cotizaciones (base recien creada con
 * schema.sql). Nunca toca datos que ya existan.
 *
 * Deja al menos un ejemplo de cada cosa:
 *   - 4 clientes mas (empresa, persona natural y uno INACTIVO) y 2 productos mas.
 *   - Cotizaciones en todos sus estados (solicitada, cotizada, contraoferta, aprobada, rechazada).
 *   - Pedidos en los 6 estados, con su historial, fechas repartidas en las
 *     ultimas semanas (para que los reportes tengan datos) y disenos en
 *     sus 3 estados (pendiente, aprobado, ajuste solicitado).
 *
 * Todas las cuentas usan la contrasena de prueba (DataSeeder.CONTRASENA_PRUEBA).
 */
@Component
public class DatosDeEjemplo {

    private static final Logger log = LoggerFactory.getLogger(DatosDeEjemplo.class);

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final EstadoPedidoRepository estadoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final DisenoRepository disenoRepository;
    private final PasswordEncoder passwordEncoder;

    private final LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);

    public DatosDeEjemplo(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository, PedidoRepository pedidoRepository,
                          DetallePedidoRepository detalleRepository, EstadoPedidoRepository estadoRepository,
                          CotizacionRepository cotizacionRepository, DisenoRepository disenoRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.estadoRepository = estadoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.disenoRepository = disenoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Se llama dentro de la transaccion de DataSeeder, despues de crear los usuarios de prueba. */
    public void sembrarSiHaceFalta() {
        if (pedidoRepository.count() > 0 || cotizacionRepository.count() > 0) {
            return; // la base ya tiene movimiento: no se toca
        }

        Usuario comercial = usuario("comercial@prueba.com");
        Usuario diseno = usuario("diseno@prueba.com");
        Usuario produccion = usuario("produccion@prueba.com");
        Usuario bodega = usuario("bodega@prueba.com");

        // ---------------- Clientes ----------------
        Cliente prueba = clienteRepository.findByCorreo("cliente@prueba.com").orElseThrow();
        Cliente espiga = cliente("900111222-3", "Panadería La Espiga", "La Espiga S.A.S.", "espiga@prueba.com",
                "3157654321", "6076431122", "Carrera 27 # 45-12, Bucaramanga", Cliente.ESTADO_ACTIVO, 60);
        Cliente cafe = cliente("900333444-5", "Café Montaña Azul", "Montaña Azul Café S.A.S.", "cafe@prueba.com",
                "3209876543", null, "Calle 36 # 29-40, Bucaramanga", Cliente.ESTADO_ACTIVO, 45);
        Cliente laura = cliente("1098765432", "Laura Gómez", null, "laura@prueba.com",
                "3014567890", null, "Calle 105 # 22-10, Floridablanca", Cliente.ESTADO_ACTIVO, 40);
        cliente("900555666-7", "Dulces Antiguos", "Dulces Antiguos Ltda.", "inactivo@prueba.com",
                "3112223344", null, "Avenida Quebradaseca # 18-20, Bucaramanga", Cliente.ESTADO_INACTIVO, 90);

        // ---------------- Productos (2 mas, para tener variedad) ----------------
        Producto cajaKraft = producto("Caja Kraft mediana", null, null, null, null);
        Producto bolsa = producto("Bolsa personalizada", null, null, null, null);
        Producto reposteria = producto("Caja para repostería", null, null, null, null);
        Producto accesorios = producto("Empaque para accesorios", null, null, null, null);
        Producto pizza = producto("Caja para pizza", "Cartón corrugado", "1900", "Cuadrada", "35x35x4 cm");
        Producto bolsaPan = producto("Bolsa de papel para pan", "Papel Kraft", "350", "Fondo cuadrado", "18x30 cm");

        // ---------------- Cotizaciones en todos sus estados ----------------
        cotizacion(prueba, null, EstadoCotizacionTipo.SOLICITADA, null, 1,
                "Caja para torta | 200 und | 25x25x12 cm | Cartón microcorrugado | Con visor y logo a una tinta");
        cotizacion(prueba, comercial, EstadoCotizacionTipo.COTIZADA, 640000.0, 3,
                "Bolsa con manija | 800 und | 20x28 cm | Papel Kraft | Logo a dos tintas");
        cotizacion(espiga, comercial, EstadoCotizacionTipo.APROBADA, 350000.0, 4,
                "Bolsa de papel para pan | 1000 und | 18x30 cm | Papel Kraft | Logo de la panadería");
        cotizacion(cafe, comercial, EstadoCotizacionTipo.RECHAZADA, 900000.0, 10,
                "Caja para café en grano | 300 und | 12x8x20 cm | Cartón rígido | Acabado mate");
        Cotizacion contraoferta = cotizacion(espiga, comercial, EstadoCotizacionTipo.CONTRAOFERTA, 480000.0, 2,
                "Caja para repostería | 1000 und | 20x20x10 cm | Cartón microcorrugado | Con visor");
        contraoferta.setValorContraoferta(420000.0);
        contraoferta.setObservaciones("Si pedimos 1.000 unidades, ¿nos la pueden dejar en $420.000?");
        cotizacionRepository.save(contraoferta);

        // ---------------- Pedidos en los 6 estados ----------------
        // 1) SOLICITADO: lo pidio el cliente desde la web (sin comercial asignado)
        Pedido p1 = pedido(prueba, null, 1, 15, "Transferencia");
        detalle(p1, bolsa, 1000);
        historial(p1, etapa(EstadoPedidoTipo.SOLICITADO, 1, null, "Pedido solicitado por el cliente desde la web"));

        // 2) EN DISENO: nace de una cotizacion aprobada; un diseno pendiente, otro aprobado
        Pedido p2 = pedido(prueba, comercial, 6, 20, "50% anticipo");
        DetallePedido p2caja = detalle(p2, cajaKraft, 500);
        DetallePedido p2bolsa = detalle(p2, bolsa, 500);
        cotizacionConPedido(prueba, comercial, 650000.0, 8,
                "Caja Kraft mediana y bolsa | 500 und c/u | Medidas de catálogo | Kraft | Logo a una tinta", p2);
        historial(p2,
                etapa(EstadoPedidoTipo.SOLICITADO, 6, comercial, "Generado desde la cotización aprobada"),
                etapa(EstadoPedidoTipo.EN_DISENO, 5, comercial, "Cliente pide el logo centrado"));
        diseno(p2caja, 1, EstadoDisenoTipo.AJUSTE_SOLICITADO, comercial, "El cliente pide el logo más grande", 4, new Color(75, 147, 182));
        diseno(p2caja, 2, EstadoDisenoTipo.PENDIENTE, diseno, null, 1, new Color(75, 147, 182));
        diseno(p2bolsa, 1, EstadoDisenoTipo.APROBADO, comercial, null, 3, new Color(17, 17, 17));

        // 3) EN PRODUCCION
        Pedido p3 = pedido(espiga, comercial, 12, 5, "Crédito 30 días");
        DetallePedido p3d = detalle(p3, reposteria, 300);
        historial(p3,
                etapa(EstadoPedidoTipo.SOLICITADO, 12, comercial, null),
                etapa(EstadoPedidoTipo.EN_DISENO, 11, comercial, null),
                etapa(EstadoPedidoTipo.EN_PRODUCCION, 7, diseno, "Diseño aprobado por el cliente"));
        diseno(p3d, 1, EstadoDisenoTipo.APROBADO, comercial, null, 8, new Color(196, 144, 82));

        // 4) TERMINADO (esperando despacho)
        Pedido p4 = pedido(cafe, comercial, 18, 2, "Contado");
        DetallePedido p4d = detalle(p4, accesorios, 200);
        historial(p4,
                etapa(EstadoPedidoTipo.SOLICITADO, 18, comercial, null),
                etapa(EstadoPedidoTipo.EN_DISENO, 17, comercial, null),
                etapa(EstadoPedidoTipo.EN_PRODUCCION, 13, diseno, null),
                etapa(EstadoPedidoTipo.TERMINADO, 3, produccion, "200 unidades listas en bodega"));
        diseno(p4d, 1, EstadoDisenoTipo.APROBADO, comercial, null, 14, new Color(34, 85, 51));

        // 5) DESPACHADO (en camino)
        Pedido p5 = pedido(prueba, comercial, 22, 0, "Transferencia");
        DetallePedido p5d = detalle(p5, pizza, 400);
        historial(p5,
                etapa(EstadoPedidoTipo.SOLICITADO, 22, comercial, null),
                etapa(EstadoPedidoTipo.EN_DISENO, 21, comercial, null),
                etapa(EstadoPedidoTipo.EN_PRODUCCION, 17, diseno, null),
                etapa(EstadoPedidoTipo.TERMINADO, 6, produccion, null),
                etapa(EstadoPedidoTipo.DESPACHADO, 1, bodega, "Sale en el camión de la mañana"));
        diseno(p5d, 1, EstadoDisenoTipo.APROBADO, comercial, null, 18, new Color(200, 40, 40));

        // 6) ENTREGADO (dos, para los reportes de tiempos)
        Pedido p6 = pedido(laura, comercial, 30, -6, "Contado");
        DetallePedido p6a = detalle(p6, bolsa, 300);
        DetallePedido p6b = detalle(p6, cajaKraft, 100);
        historial(p6,
                etapa(EstadoPedidoTipo.SOLICITADO, 30, comercial, null),
                etapa(EstadoPedidoTipo.EN_DISENO, 29, comercial, null),
                etapa(EstadoPedidoTipo.EN_PRODUCCION, 25, diseno, null),
                etapa(EstadoPedidoTipo.TERMINADO, 14, produccion, null),
                etapa(EstadoPedidoTipo.DESPACHADO, 12, bodega, null),
                etapa(EstadoPedidoTipo.ENTREGADO, 11, bodega, "Recibido por la cliente"));
        diseno(p6a, 1, EstadoDisenoTipo.APROBADO, comercial, null, 26, new Color(120, 60, 140));
        diseno(p6b, 1, EstadoDisenoTipo.APROBADO, comercial, null, 26, new Color(120, 60, 140));

        Pedido p7 = pedido(espiga, comercial, 40, -15, "Crédito 30 días");
        DetallePedido p7d = detalle(p7, bolsaPan, 2000);
        historial(p7,
                etapa(EstadoPedidoTipo.SOLICITADO, 40, comercial, null),
                etapa(EstadoPedidoTipo.EN_DISENO, 39, comercial, null),
                etapa(EstadoPedidoTipo.EN_PRODUCCION, 36, diseno, null),
                etapa(EstadoPedidoTipo.TERMINADO, 28, produccion, null),
                etapa(EstadoPedidoTipo.DESPACHADO, 27, bodega, null),
                etapa(EstadoPedidoTipo.ENTREGADO, 26, bodega, "Entregado en la panadería"));
        diseno(p7d, 1, EstadoDisenoTipo.APROBADO, comercial, null, 37, new Color(196, 144, 82));

        log.info("Datos de ejemplo creados: 4 clientes, 2 productos, 6 cotizaciones y 7 pedidos con historial y diseños.");
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    private Usuario usuario(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow();
    }

    private Cliente cliente(String nit, String nombre, String razonSocial, String correo, String celular,
                            String telefono, String direccion, String estado, int hace) {
        return clienteRepository.findById(nit).orElseGet(() -> {
            Cliente c = new Cliente();
            c.setNit(nit);
            c.setNombre(nombre);
            c.setRazonSocial(razonSocial);
            c.setCorreo(correo);
            c.setCelular(celular);
            c.setTelefono(telefono);
            c.setDireccion(direccion);
            c.setEstadoCliente(estado);
            c.setFechaRegistro(LocalDate.now().minusDays(hace));
            c.setContrasena(passwordEncoder.encode(DataSeeder.CONTRASENA_PRUEBA));
            return clienteRepository.save(c);
        });
    }

    /** Busca el producto por nombre; si no existe y se dan sus datos, lo crea. */
    private Producto producto(String nombre, String material, String precio, String forma, String tamano) {
        return productoRepository.findAll().stream()
                .filter(p -> nombre.equalsIgnoreCase(p.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    Producto p = new Producto();
                    p.setNombre(nombre);
                    p.setMaterial(material != null ? material : "Cartón Kraft");
                    p.setPrecio(new BigDecimal(precio != null ? precio : "1000"));
                    p.setForma(forma);
                    p.setTamano(tamano);
                    return productoRepository.save(p);
                });
    }

    private Cotizacion cotizacion(Cliente cliente, Usuario comercial, EstadoCotizacionTipo estado,
                                  Double valor, int hace, String especificaciones) {
        Cotizacion c = new Cotizacion();
        c.setCliente(cliente);
        c.setUsuario(comercial);
        c.setEstado(estado);
        c.setValor(valor);
        c.setFechaSolicitud(LocalDate.now().minusDays(hace));
        c.setEspecificaciones(especificaciones);
        return cotizacionRepository.save(c);
    }

    private void cotizacionConPedido(Cliente cliente, Usuario comercial, Double valor, int hace,
                                     String especificaciones, Pedido pedido) {
        Cotizacion c = cotizacion(cliente, comercial, EstadoCotizacionTipo.APROBADA, valor, hace, especificaciones);
        c.setPedido(pedido);
        cotizacionRepository.save(c);
    }

    /**
     * @param hace          dias atras en que se registro
     * @param entregaEnDias dias desde HOY para la fecha de entrega (negativo = ya paso)
     */
    private Pedido pedido(Cliente cliente, Usuario comercial, int hace, int entregaEnDias, String formaPago) {
        Pedido p = new Pedido();
        p.setCliente(cliente);
        p.setUsuario(comercial);
        p.setFechaRegistro(ahora.minusDays(hace).withHour(9));
        p.setFechaEntrega(LocalDate.now().plusDays(entregaEnDias).atStartOfDay());
        p.setFormaPago(formaPago);
        p.setDireccionEntrega(cliente.getDireccion());
        return pedidoRepository.save(p);
    }

    /** Guarda el precio de lista del momento, igual que PedidoService. */
    private DetallePedido detalle(Pedido pedido, Producto producto, int cantidad) {
        DetallePedido d = new DetallePedido();
        d.setPedido(pedido);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        return detalleRepository.save(d);
    }

    private record Etapa(EstadoPedidoTipo estado, int hace, Usuario usuario, String reporte) {
    }

    private static Etapa etapa(EstadoPedidoTipo estado, int hace, Usuario usuario, String reporte) {
        return new Etapa(estado, hace, usuario, reporte);
    }

    /**
     * Crea el historial como lo haria PedidoService.avanzarEstado: cada
     * etapa termina cuando empieza la siguiente; la ultima queda abierta
     * (o cerrada en el mismo momento si es ENTREGADO).
     */
    private void historial(Pedido pedido, Etapa... etapas) {
        for (int i = 0; i < etapas.length; i++) {
            Etapa e = etapas[i];
            EstadoPedido ep = new EstadoPedido();
            ep.setPedido(pedido);
            ep.setEstado(e.estado());
            ep.setUsuario(e.usuario());
            ep.setReporte(e.reporte());
            LocalDateTime inicio = ahora.minusDays(e.hace()).withHour(9 + i).withMinute(15);
            ep.setFechaInicio(inicio);
            if (i + 1 < etapas.length) {
                ep.setFechaFin(ahora.minusDays(etapas[i + 1].hace()).withHour(9 + i + 1).withMinute(15));
            } else if (e.estado().esFinal()) {
                ep.setFechaFin(inicio);
            }
            estadoRepository.save(ep);
        }
    }

    private void diseno(DetallePedido detalle, int version, EstadoDisenoTipo estado, Usuario usuario,
                        String observaciones, int hace, Color color) {
        Diseno d = new Diseno();
        d.setDetallePedido(detalle);
        d.setVersion(version);
        d.setEstado(estado);
        d.setUsuario(usuario);
        d.setObservaciones(observaciones);
        d.setColor("rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")");
        d.setArchivoDiseno(imagenDeEjemplo(detalle.getProducto().getNombre(), version, color));
        if (estado == EstadoDisenoTipo.APROBADO) {
            d.setFechaAprobado(LocalDate.now().minusDays(Math.max(0, hace - 1)));
        }
        disenoRepository.save(d);
    }

    /** PNG pequeno (bien por debajo de los 64 KB del BLOB) con el nombre del producto. */
    private static byte[] imagenDeEjemplo(String producto, int version, Color color) {
        try {
            BufferedImage img = new BufferedImage(320, 220, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(214, 181, 140));                 // "carton"
            g.fillRect(0, 0, 320, 220);
            g.setColor(color);
            g.fillRoundRect(30, 30, 260, 160, 18, 18);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString(producto.length() > 24 ? producto.substring(0, 24) : producto, 45, 105);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("Diseño de ejemplo · versión " + version, 45, 135);
            g.dispose();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(img, "png", salida);
            return salida.toByteArray();
        } catch (IOException | RuntimeException e) {
            return null; // sin entorno grafico: el diseno queda sin archivo, no se cae el arranque
        }
    }
}
