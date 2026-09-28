package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.SolicitarPedidoForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.DetallePedido;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.SolicitudPedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Modulo del cliente: solicitar pedidos (RF-10), ya sea de forma
 * directa o a partir de una cotizacion aprobada, y ver su detalle.
 */
@Controller
@RequestMapping("/cliente/pedidos")
public class ClientePedidoController {

    private static final String VISTA_NUEVO = "cliente/pedido-nuevo";

    private final SolicitudPedidoService solicitudPedidoService;

    public ClientePedidoController(SolicitudPedidoService solicitudPedidoService) {
        this.solicitudPedidoService = solicitudPedidoService;
    }

    private Cliente cliente(Authentication auth) {
        return ((AppUserPrincipal) auth.getPrincipal()).getCliente();
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        List<Pedido> pedidos = solicitudPedidoService.listarDeCliente(cliente(auth));
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("estados", solicitudPedidoService.estadosActuales(pedidos));
        return "cliente/pedidos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("solicitarPedidoForm", new SolicitarPedidoForm());
        model.addAttribute("productos", solicitudPedidoService.listarCatalogo());
        return VISTA_NUEVO;
    }

    /** Pedido directo desde el catalogo. */
    @PostMapping
    public String solicitarDirecto(@Valid @ModelAttribute SolicitarPedidoForm solicitarPedidoForm,
                                   BindingResult resultado, Model model, Authentication auth,
                                   RedirectAttributes redirect) {
        if (resultado.hasFieldErrors("cantidades*")) {
            model.addAttribute("errorNegocio", "Las cantidades deben ser numeros enteros.");
        } else if (!resultado.hasErrors()) {
            try {
                Pedido pedido = solicitudPedidoService.solicitarDirecto(solicitarPedidoForm, cliente(auth));
                redirect.addFlashAttribute("mensajeExito",
                        "Tu pedido PED-" + pedido.getCodigo() + " fue enviado. Te avisaremos cada vez que avance.");
                return "redirect:/cliente/pedidos/" + pedido.getCodigo();
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorNegocio", ex.getMessage());
            }
        }
        model.addAttribute("productos", solicitudPedidoService.listarCatalogo());
        return VISTA_NUEVO;
    }

    /** Pedido a partir de una cotizacion aprobada (el formulario esta en el detalle de la cotizacion). */
    @PostMapping("/desde-cotizacion/{codigoCotizacion}")
    public String solicitarDesdeCotizacion(@PathVariable Integer codigoCotizacion,
                                           @Valid @ModelAttribute SolicitarPedidoForm solicitarPedidoForm,
                                           BindingResult resultado, Authentication auth,
                                           RedirectAttributes redirect) {
        String volverACotizacion = "redirect:/cliente/cotizaciones/" + codigoCotizacion;

        FieldError errorDiseno = resultado.getFieldError("especificacionesDiseno");
        if (errorDiseno != null) {
            redirect.addFlashAttribute("errorNegocio", errorDiseno.getDefaultMessage());
            return volverACotizacion;
        }
        try {
            Pedido pedido = solicitudPedidoService.solicitarDesdeCotizacion(
                    codigoCotizacion, solicitarPedidoForm, cliente(auth));
            redirect.addFlashAttribute("mensajeExito",
                    "Tu pedido PED-" + pedido.getCodigo() + " fue creado a partir de la cotizacion COT-" + codigoCotizacion + ".");
            return "redirect:/cliente/pedidos/" + pedido.getCodigo();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorNegocio", ex.getMessage());
            return volverACotizacion;
        }
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Authentication auth, Model model) {
        Pedido pedido = solicitudPedidoService.buscarDeCliente(codigo, cliente(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<DetallePedido> detalles = solicitudPedidoService.listarDetalles(pedido);

        model.addAttribute("pedido", pedido);
        model.addAttribute("detalles", detalles);
        model.addAttribute("total", solicitudPedidoService.totalEstimado(pedido, detalles));
        model.addAttribute("estadoActual", solicitudPedidoService.estadoActual(pedido));
        model.addAttribute("historial", solicitudPedidoService.historialEstados(pedido));
        model.addAttribute("diseno", solicitudPedidoService.disenoDe(pedido).orElse(null));
        return "cliente/pedido-detalle";
    }
}
