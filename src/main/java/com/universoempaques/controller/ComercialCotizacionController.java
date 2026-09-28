package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.RegistrarValorCotizacionForm;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.model.Pedido;
import com.universoempaques.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Cotizaciones vistas por el area COMERCIAL:
 *   RF-07 registrar valor, RF-09 consultar, RF-10 generar el pedido
 *   de una cotizacion aprobada.
 */
@Controller
@RequestMapping("/comercial/cotizaciones")
public class ComercialCotizacionController {

    private final CotizacionService cotizacionService;

    public ComercialCotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) EstadoCotizacionTipo estado, Model model) {
        model.addAttribute("cotizaciones", cotizacionService.listar(estado));
        model.addAttribute("estados", EstadoCotizacionTipo.values());
        model.addAttribute("estadoFiltro", estado);
        return "interno/comercial-cotizaciones";
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Model model, RedirectAttributes flash) {
        try {
            Cotizacion cotizacion = cotizacionService.buscarPorCodigo(codigo);
            RegistrarValorCotizacionForm form = new RegistrarValorCotizacionForm();
            if (cotizacion.getValor() != null) {
                form.setValor(BigDecimal.valueOf(cotizacion.getValor()));
            }
            model.addAttribute("cotizacion", cotizacion);
            model.addAttribute("registrarValorCotizacionForm", form);
            return "interno/comercial-cotizacion-detalle";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/comercial/cotizaciones";
        }
    }

    @PostMapping("/{codigo}/valor")
    public String registrarValor(@PathVariable Integer codigo,
                                 @Valid @ModelAttribute RegistrarValorCotizacionForm registrarValorCotizacionForm,
                                 BindingResult resultado, Authentication auth, Model model,
                                 RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("cotizacion", cotizacionService.buscarPorCodigo(codigo));
            return "interno/comercial-cotizacion-detalle";
        }
        try {
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
            cotizacionService.registrarValor(codigo, registrarValorCotizacionForm, principal.getUsuario());
            flash.addFlashAttribute("exito", "Valor registrado. El cliente ya puede aprobar o rechazar la cotización.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/comercial/cotizaciones/" + codigo;
    }

    @PostMapping("/{codigo}/pedido")
    public String generarPedido(@PathVariable Integer codigo, Authentication auth, RedirectAttributes flash) {
        try {
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
            Pedido pedido = cotizacionService.generarPedido(codigo, principal.getUsuario());
            return "redirect:/comercial/pedidos/" + pedido.getCodigo();
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/comercial/cotizaciones/" + codigo;
        }
    }
}
