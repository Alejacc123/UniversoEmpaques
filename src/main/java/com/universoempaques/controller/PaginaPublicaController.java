package com.universoempaques.controller;

import com.universoempaques.dto.RegistroClienteForm;
import com.universoempaques.service.ClienteService;
import com.universoempaques.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Paginas publicas: inicio (landing) y registro de clientes (RF-02).
 * El login en si lo maneja Spring Security (ver SecurityConfig),
 * esta clase solo muestra el formulario.
 */
@Controller
public class PaginaPublicaController {

    private final ClienteService clienteService;
    private final PedidoService pedidoService;

    public PaginaPublicaController(ClienteService clienteService, PedidoService pedidoService) {
        this.clienteService = clienteService;
        this.pedidoService = pedidoService;
    }

    /** Landing: muestra el catalogo real de productos (tabla Producto). */
    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("productos", pedidoService.listarProductos());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroClienteForm", new RegistroClienteForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute RegistroClienteForm registroClienteForm,
                                    BindingResult resultado, Model model) {
        if (resultado.hasErrors()) {
            return "registro";
        }
        try {
            clienteService.registrar(registroClienteForm);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorNegocio", ex.getMessage());
            return "registro";
        }
        return "redirect:/login?registrado";
    }
}
