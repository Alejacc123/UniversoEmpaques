package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.AgregarDetalleForm;
import com.universoempaques.dto.RegistrarPedidoForm;
import com.universoempaques.model.DetallePedido;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.DisenoService;
import com.universoempaques.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Modulo del area comercial: registrar pedidos de forma directa
 * (RF-11) y agregarles los productos que incluyen.
 */
@Controller
@RequestMapping("/comercial/pedidos")
public class ComercialPedidoController {

    private final PedidoService pedidoService;
    private final CotizacionService cotizacionService;
    private final DisenoService disenoService;

    public ComercialPedidoController(PedidoService pedidoService, CotizacionService cotizacionService,
                                     DisenoService disenoService) {
        this.pedidoService = pedidoService;
        this.cotizacionService = cotizacionService;
        this.disenoService = disenoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pedidos", pedidoService.listarTodos());
        model.addAttribute("pedidoService", pedidoService); // para consultar el estado actual desde la vista
        return "interno/comercial-pedidos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("registrarPedidoForm", new RegistrarPedidoForm());
        model.addAttribute("clientes", pedidoService.listarClientesActivos());
        return "interno/comercial-pedido-nuevo";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute RegistrarPedidoForm registrarPedidoForm,
                            BindingResult resultado, Model model, Authentication auth) {
        if (resultado.hasErrors()) {
            model.addAttribute("clientes", pedidoService.listarClientesActivos());
            return "interno/comercial-pedido-nuevo";
        }
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        try {
            Pedido pedido = pedidoService.registrar(registrarPedidoForm, principal.getUsuario());
            return "redirect:/comercial/pedidos/" + pedido.getCodigo();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("clientes", pedidoService.listarClientesActivos());
            model.addAttribute("errorNegocio", ex.getMessage());
            return "interno/comercial-pedido-nuevo";
        }
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Model model) {
        AgregarDetalleForm form = new AgregarDetalleForm();
        form.setCodigoPedido(codigo);
        cargarDetalle(codigo, model);
        model.addAttribute("agregarDetalleForm", form);
        return "interno/comercial-pedido-detalle";
    }

    @PostMapping("/{codigo}/detalles")
    public String agregarDetalle(@PathVariable Integer codigo,
                                 @Valid @ModelAttribute AgregarDetalleForm agregarDetalleForm,
                                 BindingResult resultado, Model model) {
        agregarDetalleForm.setCodigoPedido(codigo);
        if (resultado.hasErrors()) {
            cargarDetalle(codigo, model);
            return "interno/comercial-pedido-detalle";
        }
        try {
            pedidoService.agregarDetalle(agregarDetalleForm);
        } catch (IllegalArgumentException ex) {
            cargarDetalle(codigo, model);
            model.addAttribute("error", ex.getMessage());
            return "interno/comercial-pedido-detalle";
        }
        return "redirect:/comercial/pedidos/" + codigo;
    }

    @PostMapping("/{codigo}/detalles/{codigoDetalle}/quitar")
    public String quitarDetalle(@PathVariable Integer codigo, @PathVariable Integer codigoDetalle,
                                RedirectAttributes flash) {
        try {
            pedidoService.quitarDetalle(codigo, codigoDetalle);
            flash.addFlashAttribute("exito", "Producto quitado del pedido.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/comercial/pedidos/" + codigo;
    }

    /** Usuario que inicio sesion (para saber si puede avanzar el pedido). */
    private static com.universoempaques.model.Usuario usuarioActual() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AppUserPrincipal p ? p.getUsuario() : null;
    }

    /** Datos que necesita la pantalla de detalle (se usa en GET y en POST con error). */
    private void cargarDetalle(Integer codigo, Model model) {
        Pedido pedido = pedidoService.buscarPorId(codigo);
        List<DetallePedido> detalles = pedidoService.listarDetalles(pedido);
        model.addAttribute("pedido", pedido);
        model.addAttribute("detalles", detalles);
        model.addAttribute("total", pedidoService.calcularTotal(detalles));
        model.addAttribute("estadoActual", pedidoService.estadoActual(pedido));
        model.addAttribute("editable", pedidoService.esEditable(pedido));
        model.addAttribute("puedeAvanzar", pedidoService.puedeAvanzar(pedido, usuarioActual()));
        model.addAttribute("historial", pedidoService.historialEstados(pedido));
        model.addAttribute("productos", pedidoService.listarProductos());
        // RF-10: si el pedido salio de una cotizacion, se muestra cual
        model.addAttribute("cotizacionOrigen", cotizacionService.buscarPorPedido(pedido).orElse(null));
        // RF-13: Comercial revisa los disenos mientras el pedido esta "En diseno"
        model.addAttribute("disenos", disenoService.listarPorPedido(pedido));
        model.addAttribute("puedeRevisarDiseno", disenoService.enEtapaDeDiseno(pedido));
    }
}
