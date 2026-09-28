package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.SolicitarCotizacionForm;
import com.universoempaques.dto.SolicitarPedidoForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.SolicitudPedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Modulo del cliente: solicitar cotizaciones (RF-06), ver el valor
 * que les asigna el area comercial y, si estan aprobadas, convertirlas
 * en pedido (RF-10).
 */
@Controller
@RequestMapping("/cliente/cotizaciones")
public class ClienteCotizacionController {

    private final CotizacionService cotizacionService;
    private final SolicitudPedidoService solicitudPedidoService;

    public ClienteCotizacionController(CotizacionService cotizacionService,
                                       SolicitudPedidoService solicitudPedidoService) {
        this.cotizacionService = cotizacionService;
        this.solicitudPedidoService = solicitudPedidoService;
    }

    private Cliente cliente(Authentication auth) {
        return ((AppUserPrincipal) auth.getPrincipal()).getCliente();
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("cotizaciones", cotizacionService.listarDeCliente(cliente(auth)));
        return "cliente/cotizaciones";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {
        model.addAttribute("solicitarCotizacionForm", new SolicitarCotizacionForm());
        return "cliente/cotizacion-nueva";
    }

    @PostMapping
    public String solicitar(@Valid @ModelAttribute SolicitarCotizacionForm solicitarCotizacionForm,
                            BindingResult resultado, Authentication auth, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "cliente/cotizacion-nueva";
        }
        Cotizacion cotizacion = cotizacionService.solicitar(solicitarCotizacionForm, cliente(auth));
        redirect.addFlashAttribute("mensajeExito",
                "Tu solicitud COT-" + cotizacion.getCodigo() + " fue enviada. El area comercial te respondera pronto.");
        return "redirect:/cliente/cotizaciones/" + cotizacion.getCodigo();
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Authentication auth, Model model) {
        Cotizacion cotizacion = cotizacionService.buscarDeCliente(codigo, cliente(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("cotizacion", cotizacion);
        model.addAttribute("pedido", solicitudPedidoService.pedidoDeCotizacion(cotizacion).orElse(null));
        model.addAttribute("solicitarPedidoForm", new SolicitarPedidoForm());
        return "cliente/cotizacion-detalle";
    }
}
