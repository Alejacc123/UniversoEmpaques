package com.universoempaques.service;

import com.universoempaques.dto.ContraofertaForm;
import com.universoempaques.dto.RegistrarValorCotizacionForm;
import com.universoempaques.dto.SolicitarCotizacionForm;
import com.universoempaques.model.*;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.CotizacionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Logica de cotizaciones (patron Facade), RF-06 a RF-10:
 *   RF-06 el cliente solicita          -> solicitar(...)
 *   RF-07 el comercial pone el valor    -> registrarValor(...)
 *   RF-08 el cliente aprueba o rechaza  -> responder(...)
 *         o propone otro valor          -> contraofertar(...) y el
 *         comercial la acepta o pone un valor nuevo
 *   RF-09 ambos consultan               -> listar... / buscar...
 *   RF-10 de la aprobada sale el pedido -> generarPedido(...)
 */
@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;
    private final ApplicationEventPublisher eventos;

    public CotizacionService(CotizacionRepository cotizacionRepository,
                             ClienteRepository clienteRepository,
                             PedidoService pedidoService,
                             ApplicationEventPublisher eventos) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
        this.eventos = eventos;
    }

    /** Guarda y avisa (Observer): NotificacionService envia el correo a quien corresponda. */
    private Cotizacion guardarYAvisar(Cotizacion cotizacion, boolean porComercial) {
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        eventos.publishEvent(new CotizacionCambiadaEvent(guardada.getCodigo(), guardada.getEstado(), porComercial));
        return guardada;
    }

    // ------------------------------------------------------------------
    // Cliente
    // ------------------------------------------------------------------

    /** RF-06: el cliente solicita una cotizacion describiendo el empaque. */
    @Transactional
    public Cotizacion solicitar(SolicitarCotizacionForm form, String nitCliente) {
        Cliente cliente = buscarCliente(nitCliente);
        byte[] foto = leerFoto(form.getFoto());   // valida antes de guardar nada

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setEspecificaciones(armarEspecificaciones(form));
        cotizacion.setEstado(EstadoCotizacionTipo.SOLICITADA);
        cotizacion.setFechaSolicitud(LocalDate.now());
        cotizacion.setFotoReferencia(foto);
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
    public Cotizacion responder(Integer codigo, String nitCliente, boolean aprobar, String observaciones) {
        Cotizacion cotizacion = buscarPendienteDeRespuesta(codigo, nitCliente);
        cotizacion.setEstado(aprobar ? EstadoCotizacionTipo.APROBADA : EstadoCotizacionTipo.RECHAZADA);
        cotizacion.setObservaciones(limpiar(observaciones));
        return guardarYAvisar(cotizacion, false);
    }

    /**
     * Contraoferta: el cliente propone otro valor (y puede explicar por que).
     * La cotizacion vuelve a Comercial en estado CONTRAOFERTA.
     */
    @Transactional
    public Cotizacion contraofertar(Integer codigo, String nitCliente, ContraofertaForm form) {
        Cotizacion cotizacion = buscarPendienteDeRespuesta(codigo, nitCliente);
        double propuesto = form.getValor().doubleValue();
        if (cotizacion.getValor() != null && propuesto == cotizacion.getValor()) {
            throw new IllegalArgumentException("El valor que propones es igual al cotizado; si estás de acuerdo, apruébala.");
        }
        cotizacion.setValorContraoferta(propuesto);
        cotizacion.setObservaciones(limpiar(form.getObservaciones()));
        cotizacion.setEstado(EstadoCotizacionTipo.CONTRAOFERTA);
        return guardarYAvisar(cotizacion, false);
    }

    private Cotizacion buscarPendienteDeRespuesta(Integer codigo, String nitCliente) {
        Cotizacion cotizacion = buscarDelCliente(codigo, nitCliente);
        if (cotizacion.getEstado() != EstadoCotizacionTipo.COTIZADA) {
            throw new IllegalArgumentException("Solo puedes responder una cotización que ya tenga valor y esté pendiente.");
        }
        return cotizacion;
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
                && cotizacion.getEstado() != EstadoCotizacionTipo.COTIZADA
                && cotizacion.getEstado() != EstadoCotizacionTipo.CONTRAOFERTA) {
            throw new IllegalArgumentException("Esta cotización ya fue respondida por el cliente; no se puede cambiar el valor.");
        }
        cotizacion.setValor(form.getValor().doubleValue());
        cotizacion.setUsuario(comercial);
        cotizacion.setEstado(EstadoCotizacionTipo.COTIZADA);
        return guardarYAvisar(cotizacion, true);
    }

    /**
     * El comercial acepta el valor que propuso el cliente: ese pasa a ser
     * el valor de la cotizacion y queda APROBADA (lista para el pedido).
     */
    @Transactional
    public Cotizacion aceptarContraoferta(Integer codigo, Usuario comercial) {
        Cotizacion cotizacion = buscarPorCodigo(codigo);
        if (cotizacion.getEstado() != EstadoCotizacionTipo.CONTRAOFERTA || cotizacion.getValorContraoferta() == null) {
            throw new IllegalArgumentException("Esta cotización no tiene una contraoferta pendiente.");
        }
        cotizacion.setValor(cotizacion.getValorContraoferta());
        cotizacion.setUsuario(comercial);
        cotizacion.setEstado(EstadoCotizacionTipo.APROBADA);
        return guardarYAvisar(cotizacion, true);
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

    /** Foto opcional: PNG o JPG (se revisa la firma del archivo, no la extension), hasta 5 MB. */
    static byte[] leerFoto(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return null;
        }
        if (foto.getSize() > MAX_FOTO) {
            throw new IllegalArgumentException("La foto pesa más de 5 MB.");
        }
        byte[] b;
        try {
            b = foto.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la foto. Intenta de nuevo.");
        }
        if (!esImagen(b)) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPG o PNG.");
        }
        return b;
    }

    static final long MAX_FOTO = 5L * 1024 * 1024;

    static boolean esImagen(byte[] b) {
        if (b == null || b.length < 4) return false;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean jpg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        return png || jpg;
    }

    /** Tipo de contenido para mostrar la foto en el navegador. */
    public static String tipoDeImagen(byte[] b) {
        return b != null && b.length > 0 && (b[0] & 0xFF) == 0x89 ? "image/png" : "image/jpeg";
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }

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
