package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.config.RedireccionSegunRolHandler;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.service.BackupService;
import com.universoempaques.service.ClienteService;
import com.universoempaques.service.CotizacionService;
import com.universoempaques.service.PedidoService;
import com.universoempaques.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paneles de inicio de cada rol/cargo (RF-01: redireccion post-login),
 * con los numeros principales de cada area.
 */
@Controller
public class PanelController {

    private final CotizacionService cotizacionService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final BackupService backupService;

    public PanelController(CotizacionService cotizacionService, PedidoService pedidoService,
                           UsuarioService usuarioService, ClienteService clienteService,
                           BackupService backupService) {
        this.cotizacionService = cotizacionService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.backupService = backupService;
    }

    private AppUserPrincipal principal(Authentication auth) {
        return (AppUserPrincipal) auth.getPrincipal();
    }

    /** El logo de la barra superior lleva aqui: cada quien a su propio panel. */
    @GetMapping("/mi-panel")
    public String miPanel(Authentication auth) {
        return "redirect:" + RedireccionSegunRolHandler.destinoPara(auth);
    }

    @GetMapping("/cliente/panel")
    public String panelCliente(Authentication auth, Model model) {
        String nit = principal(auth).getCliente().getNit();
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("cotizacionesPorResponder", cotizacionService.contarPendientesDeRespuesta(nit));
        model.addAttribute("totalCotizaciones", cotizacionService.listarDelCliente(nit).size());
        model.addAttribute("totalPedidos", pedidoService.contarPorCliente(nit));
        return "cliente/panel";
    }

    @GetMapping("/comercial/panel")
    public String panelComercial(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("porCotizar", cotizacionService.contarPorEstado(EstadoCotizacionTipo.SOLICITADA));
        model.addAttribute("esperandoCliente", cotizacionService.contarPorEstado(EstadoCotizacionTipo.COTIZADA));
        model.addAttribute("aprobadasSinPedido", cotizacionService.contarAprobadasSinPedido());
        model.addAttribute("totalPedidos", pedidoService.listarTodos().size());
        model.addAttribute("totalClientes", clienteService.listarTodos().size());
        model.addAttribute("porEnviarADiseno", pedidoService.contarEnEstados(java.util.List.of(com.universoempaques.model.EstadoPedidoTipo.SOLICITADO)));
        return "interno/panel-comercial";
    }

    @GetMapping("/diseno/panel")
    public String panelDiseno(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("enCola", pedidoService.contarEnEstados(AreaPedidoController.colaDe("diseno")));
        return "interno/panel-diseno";
    }

    @GetMapping("/produccion/panel")
    public String panelProduccion(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("enCola", pedidoService.contarEnEstados(AreaPedidoController.colaDe("produccion")));
        return "interno/panel-produccion";
    }

    @GetMapping("/bodega/panel")
    public String panelBodega(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("enCola", pedidoService.contarEnEstados(AreaPedidoController.colaDe("bodega")));
        return "interno/panel-bodega";
    }

    @GetMapping("/admin/panel")
    public String panelAdmin(Authentication auth, Model model) {
        model.addAttribute("nombre", principal(auth).getNombreParaSaludo());
        model.addAttribute("totalUsuarios", usuarioService.listarTodos().size());
        model.addAttribute("totalClientes", clienteService.listarTodos().size());
        model.addAttribute("totalPedidos", pedidoService.listarTodos().size());
        var archivos = backupService.listarArchivos();
        model.addAttribute("ultimoRespaldo", archivos.isEmpty() ? null : archivos.get(0).modificado());
        return "interno/panel-admin";
    }
}
