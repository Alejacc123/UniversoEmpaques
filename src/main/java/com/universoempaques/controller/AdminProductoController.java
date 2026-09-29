package com.universoempaques.controller;

import com.universoempaques.dto.ProductoForm;
import com.universoempaques.model.Producto;
import com.universoempaques.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Catalogo de productos: el administrador agrega, edita (precio, tamano...)
 * y elimina productos. Solo rol ADMIN (/admin/**).
 */
@Controller
@RequestMapping("/admin/productos")
public class AdminProductoController {

    private final ProductoService productoService;

    public AdminProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Producto> productos = productoService.listar();
        model.addAttribute("productos", productos);
        model.addAttribute("usos", productoService.usosPorProducto(productos));
        return "interno/admin-productos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("productoForm", new ProductoForm());
        return "interno/admin-producto-form";
    }

    @GetMapping("/{codigo}/editar")
    public String editar(@PathVariable Integer codigo, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("productoForm", productoService.formDe(productoService.buscar(codigo)));
            return "interno/admin-producto-form";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/productos";
        }
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute ProductoForm productoForm, BindingResult resultado,
                          Model model, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return "interno/admin-producto-form";
        }
        try {
            boolean nuevo = productoForm.getCodigo() == null;
            Producto p = productoService.guardar(productoForm);
            flash.addFlashAttribute("exito", (nuevo ? "Producto agregado: " : "Producto actualizado: ") + p.getNombre()
                    + (nuevo ? "." : ". Los pedidos anteriores conservan su precio."));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "interno/admin-producto-form";
        }
        return "redirect:/admin/productos";
    }

    @PostMapping("/{codigo}/eliminar")
    public String eliminar(@PathVariable Integer codigo, RedirectAttributes flash) {
        try {
            productoService.eliminar(codigo);
            flash.addFlashAttribute("exito", "Producto eliminado del catálogo.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/productos";
    }
}
