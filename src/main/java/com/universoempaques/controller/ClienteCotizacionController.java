package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.ContraofertaForm;
import com.universoempaques.dto.SolicitarCotizacionForm;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cotizaciones vistas por el CLIENTE:
 *   RF-06 solicitar, RF-08 aprobar/rechazar, RF-09 consultar.
 * Siempre se trabaja con el NIT de quien inicio sesion, nunca con
 * uno que venga del formulario o de la URL.
 */
@Controller
@RequestMapping("/cliente/cotizaciones")
public class ClienteCotizacionController {

    private final CotizacionService cotizacionService;
    private final PedidoService pedidoService;

    public ClienteCotizacionController(CotizacionService cotizacionService, PedidoService pedidoService) {
        this.cotizacionService = cotizacionService;
        this.pedidoService = pedidoService;
    }

    private String nit(Authentication auth) {
        return ((AppUserPrincipal) auth.getPrincipal()).getCliente().getNit();
    }

    @GetMapping
    public String listar(Authentication auth, Model model) {
        model.addAttribute("cotizaciones", cotizacionService.listarDelCliente(nit(auth)));
        return "cliente/cotizaciones";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {
        model.addAttribute("solicitarCotizacionForm", new SolicitarCotizacionForm());
        model.addAttribute("productos", pedidoService.listarProductos());
        return "cliente/cotizacion-nueva";
    }

    @PostMapping
    public String solicitar(@Valid @ModelAttribute SolicitarCotizacionForm solicitarCotizacionForm,
                            BindingResult resultado, Authentication auth, Model model) {
        if (!resultado.hasErrors()) {
            try {
                Cotizacion cotizacion = cotizacionService.solicitar(solicitarCotizacionForm, nit(auth));
                return "redirect:/cliente/cotizaciones/" + cotizacion.getCodigo() + "?creada";
            } catch (IllegalArgumentException ex) {
                resultado.rejectValue("foto", "foto", ex.getMessage());
            }
        }
        model.addAttribute("productos", pedidoService.listarProductos());
        return "cliente/cotizacion-nueva";
    }

    @GetMapping("/{codigo}")
    public String verDetalle(@PathVariable Integer codigo, Authentication auth, Model model,
                             RedirectAttributes flash) {
        try {
            model.addAttribute("cotizacion", cotizacionService.buscarDelCliente(codigo, nit(auth)));
            if (!model.containsAttribute("contraofertaForm")) {
                model.addAttribute("contraofertaForm", new ContraofertaForm());
            }
            return "cliente/cotizacion-detalle";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cliente/cotizaciones";
        }
    }

    @PostMapping("/{codigo}/aprobar")
    public String aprobar(@PathVariable Integer codigo, @RequestParam(required = false) String observaciones,
                          Authentication auth, RedirectAttributes flash) {
        return responder(codigo, true, observaciones, auth, flash);
    }

    @PostMapping("/{codigo}/rechazar")
    public String rechazar(@PathVariable Integer codigo, @RequestParam(required = false) String observaciones,
                           Authentication auth, RedirectAttributes flash) {
        return responder(codigo, false, observaciones, auth, flash);
    }

    /** Contraoferta: el cliente propone otro valor; vuelve a Comercial. */
    @PostMapping("/{codigo}/contraoferta")
    public String contraofertar(@PathVariable Integer codigo, @Valid @ModelAttribute ContraofertaForm contraofertaForm,
                                BindingResult resultado, Authentication auth, Model model, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("cotizacion", cotizacionService.buscarDelCliente(codigo, nit(auth)));
            model.addAttribute("abrirContraoferta", true);
            return "cliente/cotizacion-detalle";
        }
        try {
            cotizacionService.contraofertar(codigo, nit(auth), contraofertaForm);
            flash.addFlashAttribute("exito", "Contraoferta enviada. El área comercial la revisará y te responderá.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cliente/cotizaciones/" + codigo;
    }

    /** Foto de referencia, solo de las cotizaciones de este cliente. */
    @GetMapping("/{codigo}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Integer codigo, Authentication auth) {
        try {
            byte[] foto = cotizacionService.buscarDelCliente(codigo, nit(auth)).getFotoReferencia();
            if (foto == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(CotizacionService.tipoDeImagen(foto)))
                    .cacheControl(CacheControl.noStore()).body(foto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private String responder(Integer codigo, boolean aprobar, String observaciones, Authentication auth,
                             RedirectAttributes flash) {
        try {
            cotizacionService.responder(codigo, nit(auth), aprobar, observaciones);
            flash.addFlashAttribute("exito", aprobar
                    ? "¡Cotización aprobada! El área comercial generará tu pedido."
                    : "Cotización rechazada.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cliente/cotizaciones/" + codigo;
    }
}
