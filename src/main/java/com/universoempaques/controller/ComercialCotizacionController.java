package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.RegistrarCotizacionForm;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Modulo del area comercial: consultar las cotizaciones solicitadas
 * por los clientes y registrar su valor y condiciones (RF-07).
 */
@Controller
@RequestMapping("/comercial/cotizaciones")
public class ComercialCotizacionController {

    private static final String VISTA_DETALLE = "interno/comercial-cotizacion-detalle";

    private final CotizacionService cotizacionService;

    public ComercialCotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) EstadoCotizacionTipo estado, Model model) {
        model.addAttribute("cotizaciones", cotizacionService.listar(estado));
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("estados", EstadoCotizacionTipo.values());
        model.addAttribute("pendientes", cotizacionService.contarPorEstado(EstadoCotizacionTipo.SOLICITADA));
        return "interno/comercial-cotizaciones";
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Model model) {
        model.addAttribute("cotizacion", cotizacionService.buscarPorId(codigo));
        model.addAttribute("registrarCotizacionForm", new RegistrarCotizacionForm());
        return VISTA_DETALLE;
    }

    @PostMapping("/{codigo}/registrar")
    public String registrar(@PathVariable Integer codigo,
                            @Valid @ModelAttribute RegistrarCotizacionForm registrarCotizacionForm,
                            BindingResult resultado, Model model, Authentication auth,
                            RedirectAttributes redirect) {
        Cotizacion cotizacion = cotizacionService.buscarPorId(codigo);
        if (resultado.hasErrors()) {
            model.addAttribute("cotizacion", cotizacion);
            return VISTA_DETALLE;
        }
        try {
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
            cotizacionService.registrar(codigo, registrarCotizacionForm, principal.getUsuario());
        } catch (IllegalStateException ex) {
            model.addAttribute("cotizacion", cotizacion);
            model.addAttribute("errorNegocio", ex.getMessage());
            return VISTA_DETALLE;
        }
        redirect.addFlashAttribute("mensajeExito", "Cotizacion COT-" + codigo + " registrada correctamente.");
        return "redirect:/comercial/cotizaciones/" + codigo;
    }
}
