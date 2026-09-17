package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;

/**
 * Paneles de entrada de cada rol/cargo (RF-01: redireccion post-login).
 * Por ahora cada panel es una pantalla de bienvenida; en las siguientes
 * iteraciones se agregan los modulos propios de cada area
 * (cotizaciones, pedidos, diseno, produccion, despacho, reportes...).
 */
@Controller
public class PanelController {

    private AppUserPrincipal principal(Authentication auth) {
        return (AppUserPrincipal) auth.getPrincipal();
    }

    @GetMapping("/cliente/panel")
    public String panelCliente(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
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
