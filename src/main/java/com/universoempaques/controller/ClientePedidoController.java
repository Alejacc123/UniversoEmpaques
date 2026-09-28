package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.SolicitarPedidoForm;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.DisenoService;
import com.universoempaques.service.PedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * RF-15: el CLIENTE consulta el estado y el avance de sus pedidos.
 * Solo ve los suyos (se usa el NIT de quien inicio sesion).
 */
@Controller
@RequestMapping("/cliente/pedidos")
public class ClientePedidoController {

    private final PedidoService pedidoService;
    private final CotizacionService cotizacionService;
    private final DisenoService disenoService;

    public ClientePedidoController(PedidoService pedidoService, CotizacionService cotizacionService,
                                   DisenoService disenoService) {
        this.pedidoService = pedidoService;
        this.cotizacionService = cotizacionService;
        this.disenoService = disenoService;
    }

    private String nit(Authentication auth) {
        return ((AppUserPrincipal) auth.getPrincipal()).getCliente().getNit();
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("pedidos", pedidoService.listarDelCliente(nit(auth)));
        model.addAttribute("pedidoService", pedidoService);
        return "cliente/pedidos";
    }

    /** RF-10: formulario para pedir directamente, sin cotizacion. */
    @GetMapping("/nuevo")
    public String nuevo(Authentication auth, Model model) {
        SolicitarPedidoForm form = new SolicitarPedidoForm();
        form.setDireccionEntrega(((AppUserPrincipal) auth.getPrincipal()).getCliente().getDireccion());
        return mostrarFormulario(model, form);
    }

    private String mostrarFormulario(Model model, SolicitarPedidoForm form) {
        model.addAttribute("solicitarPedidoForm", form);
        model.addAttribute("productos", pedidoService.listarProductos());
        return "cliente/pedido-nuevo";
    }

    @PostMapping
    public String solicitar(@Valid @ModelAttribute SolicitarPedidoForm solicitarPedidoForm, BindingResult resultado,
                            Authentication auth, Model model) {
        if (resultado.hasErrors()) {
            return mostrarFormulario(model, solicitarPedidoForm);
        }
        try {
            Pedido pedido = pedidoService.solicitarPorCliente(solicitarPedidoForm, nit(auth));
            return "redirect:/cliente/pedidos/" + pedido.getCodigo() + "?creado";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return mostrarFormulario(model, solicitarPedidoForm);
        }
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Authentication auth, Model model, RedirectAttributes flash) {
        try {
            Pedido pedido = pedidoService.buscarDelCliente(codigo, nit(auth));
            var detalles = pedidoService.listarDetalles(pedido);
            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            model.addAttribute("total", pedidoService.calcularTotal(detalles));
            model.addAttribute("estadoActual", pedidoService.estadoActual(pedido));
            model.addAttribute("historial", pedidoService.historialEstados(pedido));
            model.addAttribute("cotizacionOrigen", cotizacionService.buscarPorPedido(pedido).orElse(null));
            var disenos = disenoService.listarPorPedido(pedido);
            model.addAttribute("disenos", disenos);
            model.addAttribute("hayDisenos", disenos.stream().anyMatch(g -> g.getUltimo() != null));
            return "cliente/pedido-detalle";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cliente/pedidos";
        }
    }
}
