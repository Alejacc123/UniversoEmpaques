package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.AgregarDetalleForm;
import com.universoempaques.dto.RegistrarPedidoForm;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Modulo del area comercial: registrar pedidos de forma directa
 * (RF-11) y agregarles los productos que incluyen.
 */
@Controller
@RequestMapping("/comercial/pedidos")
public class ComercialPedidoController {

    private final PedidoService pedidoService;

    public ComercialPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
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
        model.addAttribute("clientes", pedidoService.listarClientes());
        return "interno/comercial-pedido-nuevo";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute RegistrarPedidoForm registrarPedidoForm,
                            BindingResult resultado, Model model, Authentication auth) {
        if (resultado.hasErrors()) {
            model.addAttribute("clientes", pedidoService.listarClientes());
            return "interno/comercial-pedido-nuevo";
        }
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        Pedido pedido = pedidoService.registrar(registrarPedidoForm, principal.getUsuario());
        return "redirect:/comercial/pedidos/" + pedido.getCodigo();
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Model model) {
        Pedido pedido = pedidoService.buscarPorId(codigo);

        AgregarDetalleForm form = new AgregarDetalleForm();
        form.setCodigoPedido(codigo);

        model.addAttribute("pedido", pedido);
        model.addAttribute("detalles", pedidoService.listarDetalles(pedido));
        model.addAttribute("estadoActual", pedidoService.estadoActual(pedido));
        model.addAttribute("productos", pedidoService.listarProductos());
        model.addAttribute("agregarDetalleForm", form);
        return "interno/comercial-pedido-detalle";
    }

    @PostMapping("/{codigo}/detalles")
    public String agregarDetalle(@PathVariable Integer codigo,
                                 @Valid @ModelAttribute AgregarDetalleForm agregarDetalleForm,
                                 BindingResult resultado, Model model) {
        agregarDetalleForm.setCodigoPedido(codigo);
        if (resultado.hasErrors()) {
            Pedido pedido = pedidoService.buscarPorId(codigo);
            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", pedidoService.listarDetalles(pedido));
            model.addAttribute("estadoActual", pedidoService.estadoActual(pedido));
            model.addAttribute("productos", pedidoService.listarProductos());
            return "interno/comercial-pedido-detalle";
        }
        pedidoService.agregarDetalle(agregarDetalleForm);
        return "redirect:/comercial/pedidos/" + codigo;
    }
}