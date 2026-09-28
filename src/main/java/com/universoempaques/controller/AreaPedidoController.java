package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.model.EstadoPedido;
import com.universoempaques.model.EstadoPedidoTipo;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.DisenoService;
import com.universoempaques.service.PedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pedidos vistos por las areas de Diseno, Produccion y Bodega.
 *   RF-14: cada area hace avanzar el pedido en SU etapa.
 *   RF-15: los usuarios internos consultan el estado y el avance.
 *   RF-17: Bodega registra el despacho y la entrega.
 *
 * Una sola clase para las tres areas: la URL dice cual es
 * (/diseno/pedidos, /produccion/pedidos, /bodega/pedidos) y
 * SecurityConfig ya limita quien entra a cada una.
 */
@Controller
public class AreaPedidoController {

    /** Que estados le tocan a cada area (su "cola de trabajo"). */
    private static final Map<String, List<EstadoPedidoTipo>> COLA = Map.of(
            "diseno", List.of(EstadoPedidoTipo.EN_DISENO),
            "produccion", List.of(EstadoPedidoTipo.EN_PRODUCCION),
            "bodega", List.of(EstadoPedidoTipo.TERMINADO, EstadoPedidoTipo.DESPACHADO));

    private static final Map<String, String> TITULO = Map.of(
            "diseno", "Diseño", "produccion", "Producción", "bodega", "Bodega", "comercial", "Comercial");

    private final PedidoService pedidoService;
    private final CotizacionService cotizacionService;
    private final DisenoService disenoService;

    public AreaPedidoController(PedidoService pedidoService, CotizacionService cotizacionService,
                                DisenoService disenoService) {
        this.pedidoService = pedidoService;
        this.cotizacionService = cotizacionService;
        this.disenoService = disenoService;
    }

    public static List<EstadoPedidoTipo> colaDe(String area) {
        return COLA.getOrDefault(area, List.of());
    }

    @GetMapping("/{area:diseno|produccion|bodega}/pedidos")
    public String listar(@PathVariable String area, @RequestParam(defaultValue = "false") boolean todos, Model model) {
        List<EstadoPedido> filas = todos
                ? pedidoService.listarTodos().stream().map(pedidoService::estadoActual).filter(Objects::nonNull).toList()
                : pedidoService.pedidosEnEstados(colaDe(area));
        model.addAttribute("area", area);
        model.addAttribute("tituloArea", TITULO.get(area));
        model.addAttribute("todos", todos);
        model.addAttribute("filas", filas);
        model.addAttribute("estadosCola", colaDe(area));
        return "interno/area-pedidos";
    }

    @GetMapping("/{area:diseno|produccion|bodega}/pedidos/{codigo}")
    public String verDetalle(@PathVariable String area, @PathVariable Integer codigo, Authentication auth,
                             Model model, RedirectAttributes flash) {
        try {
            Pedido pedido = pedidoService.buscarPorId(codigo);
            var detalles = pedidoService.listarDetalles(pedido);
            model.addAttribute("area", area);
            model.addAttribute("tituloArea", TITULO.get(area));
            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            model.addAttribute("total", pedidoService.calcularTotal(detalles));
            model.addAttribute("estadoActual", pedidoService.estadoActual(pedido));
            model.addAttribute("historial", pedidoService.historialEstados(pedido));
            model.addAttribute("puedeAvanzar", pedidoService.puedeAvanzar(pedido, usuario(auth)));
            model.addAttribute("cotizacionOrigen", cotizacionService.buscarPorPedido(pedido).orElse(null));
            // RF-12: Diseno sube archivos solo en su propia pantalla y con el pedido "En diseno"
            model.addAttribute("disenos", disenoService.listarPorPedido(pedido));
            model.addAttribute("puedeSubirDiseno", "diseno".equals(area) && disenoService.enEtapaDeDiseno(pedido));
            return "interno/pedido-seguimiento";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/" + area + "/pedidos";
        }
    }

    /**
     * RF-14 / RF-17: pasar el pedido a la siguiente etapa. Tambien lo usa
     * Comercial (SOLICITADO -> EN_DISENO) desde su pantalla de pedido.
     */
    @PostMapping("/{area:comercial|diseno|produccion|bodega}/pedidos/{codigo}/avanzar")
    public String avanzar(@PathVariable String area, @PathVariable Integer codigo,
                          @RequestParam(required = false) String reporte,
                          Authentication auth, RedirectAttributes flash) {
        try {
            EstadoPedido nuevo = pedidoService.avanzarEstado(codigo, usuario(auth), reporte);
            flash.addFlashAttribute("exito", "Listo: el pedido PED-" + codigo + " pasó a \""
                    + nuevo.getEstado().getEtiqueta() + "\". Se notificó al cliente.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/" + area + "/pedidos/" + codigo;
    }

    private static com.universoempaques.model.Usuario usuario(Authentication auth) {
        return ((AppUserPrincipal) auth.getPrincipal()).getUsuario();
    }
}
