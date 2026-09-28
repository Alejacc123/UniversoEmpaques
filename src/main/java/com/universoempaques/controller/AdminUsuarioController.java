package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.RegistrarUsuarioForm;
import com.universoempaques.model.Usuario;
import com.universoempaques.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Modulo de administracion: registrar trabajadores (RF-03) y
 * definir su cargo/permisos (RF-04).
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "interno/admin-usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        cargarListasDeApoyo(model);
        model.addAttribute("registrarUsuarioForm", new RegistrarUsuarioForm());
        model.addAttribute("esEdicion", false);
        return "interno/admin-usuario-form";
    }

    @GetMapping("/{codigo}/editar")
    public String mostrarFormularioEditar(@PathVariable Integer codigo, Model model) {
        Usuario usuario = usuarioService.buscarPorId(codigo);

        RegistrarUsuarioForm form = new RegistrarUsuarioForm();
        form.setCodigo(usuario.getCodigo());
        form.setNombre(usuario.getNombre());
        form.setCorreo(usuario.getCorreo());
        form.setNumDocumento(usuario.getNumDocumento());
        form.setTelefonoPersonal(usuario.getTelefonoPersonal());
        form.setTelefonoEmpresa(usuario.getTelefonoEmpresa());
        form.setFechaIngreso(usuario.getFechaIngreso());
        form.setFecVencimientoContrato(usuario.getFecVencimientoContrato());
        form.setCantHorasTrabajadas(usuario.getCantHorasTrabajadas());
        form.setNomina(usuario.getNomina());
        // Modelo v2: rol y area salen de las tablas puente (el principal)
        form.setCodigoRol(usuario.getRolPrincipal() != null ? usuario.getRolPrincipal().getCodigo() : null);
        form.setCodigoArea(usuario.getAreaPrincipal() != null ? usuario.getAreaPrincipal().getCodigo() : null);

        cargarListasDeApoyo(model);
        model.addAttribute("registrarUsuarioForm", form);
        model.addAttribute("esEdicion", true);
        return "interno/admin-usuario-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute RegistrarUsuarioForm registrarUsuarioForm,
                          BindingResult resultado, Model model, Authentication auth) {
        if (resultado.hasErrors()) {
            cargarListasDeApoyo(model);
            model.addAttribute("esEdicion", registrarUsuarioForm.getCodigo() != null);
            return "interno/admin-usuario-form";
        }
        try {
            Integer quienEdita = ((AppUserPrincipal) auth.getPrincipal()).getUsuario().getCodigo();
            usuarioService.guardar(registrarUsuarioForm, quienEdita);
        } catch (IllegalArgumentException ex) {
            cargarListasDeApoyo(model);
            model.addAttribute("esEdicion", registrarUsuarioForm.getCodigo() != null);
            model.addAttribute("errorNegocio", ex.getMessage());
            return "interno/admin-usuario-form";
        }
        return "redirect:/admin/usuarios?guardado";
    }

    @PostMapping("/{codigo}/eliminar")
    public String eliminar(@PathVariable Integer codigo, Authentication auth, RedirectAttributes flash) {
        Integer quienElimina = ((AppUserPrincipal) auth.getPrincipal()).getUsuario().getCodigo();
        try {
            usuarioService.eliminar(codigo, quienElimina);
            flash.addFlashAttribute("exito", "Usuario eliminado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    private void cargarListasDeApoyo(Model model) {
        model.addAttribute("areas", usuarioService.listarAreas());
        model.addAttribute("roles", usuarioService.listarRoles());
    }
}