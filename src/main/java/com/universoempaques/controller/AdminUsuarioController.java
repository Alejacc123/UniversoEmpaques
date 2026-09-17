package com.universoempaques.controller;

import com.universoempaques.dto.RegistrarUsuarioForm;
import com.universoempaques.model.Usuario;
import com.universoempaques.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
        Usuario usuario = usuarioService.listarTodos().stream()
                .filter(u -> u.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        RegistrarUsuarioForm form = new RegistrarUsuarioForm();
        form.setCodigo(usuario.getCodigo());
        form.setNombre(usuario.getNombre());
        form.setCorreo(usuario.getCorreo());
        form.setTelefono(usuario.getTelefono() != null ? String.valueOf(usuario.getTelefono()) : "");
        form.setCodigoRol(usuario.getRol() != null ? usuario.getRol().getCodigo() : null);
        form.setCodigoArea(usuario.getArea() != null ? usuario.getArea().getCodigo() : null);

        cargarListasDeApoyo(model);
        model.addAttribute("registrarUsuarioForm", form);
        model.addAttribute("esEdicion", true);
        return "interno/admin-usuario-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute RegistrarUsuarioForm registrarUsuarioForm,
                          BindingResult resultado, Model model) {
        if (resultado.hasErrors()) {
            cargarListasDeApoyo(model);
            model.addAttribute("esEdicion", registrarUsuarioForm.getCodigo() != null);
            return "interno/admin-usuario-form";
        }
        try {
            usuarioService.guardar(registrarUsuarioForm);
        } catch (IllegalArgumentException ex) {
            cargarListasDeApoyo(model);
            model.addAttribute("esEdicion", registrarUsuarioForm.getCodigo() != null);
            model.addAttribute("errorNegocio", ex.getMessage());
            return "interno/admin-usuario-form";
        }
        return "redirect:/admin/usuarios";
    }

    private void cargarListasDeApoyo(Model model) {
        model.addAttribute("areas", usuarioService.listarAreas());
        model.addAttribute("roles", usuarioService.listarRoles());
    }
}