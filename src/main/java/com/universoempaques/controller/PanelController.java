package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.SolicitudPedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paneles de entrada de cada rol/cargo (RF-01: redireccion post-login).
 * En las siguientes iteraciones se agregan los modulos propios de cada
 * area (diseno, produccion, despacho, reportes...).
 */
@Controller
public class PanelController {

    private final CotizacionService cotizacionService;
    private final SolicitudPedidoService solicitudPedidoService;

    public PanelController(CotizacionService cotizacionService,
                           SolicitudPedidoService solicitudPedidoService) {
        this.cotizacionService = cotizacionService;
        this.solicitudPedidoService = solicitudPedidoService;
    }

    private AppUserPrincipal principal(Authentication auth) {
        return (AppUserPrincipal) auth.getPrincipal();
    }

    @GetMapping("/cliente/panel")
    public String panelCliente(Authentication auth, Model model) {
        AppUserPrincipal principal = principal(auth);
        Cliente cliente = principal.getCliente();
        model.addAttribute("nombre", principal.getNombreParaSaludo());
        model.addAttribute("cotizacionesEnRevision",
                cotizacionService.contarDeClientePorEstado(cliente, EstadoCotizacionTipo.SOLICITADA));
        model.addAttribute("cotizacionesConValor",
                cotizacionService.contarDeClientePorEstado(cliente, EstadoCotizacionTipo.REGISTRADA));
        model.addAttribute("pedidosActivos", solicitudPedidoService.contarActivos(cliente));
        model.addAttribute("pedidosEntregados", solicitudPedidoService.contarEntregados(cliente));
        return "cliente/panel";
    }

    @GetMapping("/comercial/panel")
    public String panelComercial(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        return "interno/panel-comercial";
    }

    @GetMapping("/diseno/panel")
    public String panelDiseno(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        return "interno/panel-diseno";
    }

    @GetMapping("/produccion/panel")
    public String panelProduccion(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        return "interno/panel-produccion";
    }

    @GetMapping("/bodega/panel")
    public String panelBodega(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        return "interno/panel-bodega";
    }

    @GetMapping("/admin/panel")
    public String panelAdmin(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        return "interno/panel-admin";
    }
}