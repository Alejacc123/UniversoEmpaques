package com.universoempaques.controller;

import com.universoempaques.dto.EditarClienteForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Modulo del area comercial: consultar y administrar la
 * informacion de las cuentas de los clientes registrados (RF-05).
 */
@Controller
@RequestMapping("/comercial/clientes")
public class ComercialClienteController {

    private final ClienteService clienteService;

    public ComercialClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "interno/comercial-clientes";
    }

    @GetMapping("/{codigo}/editar")
    public String mostrarFormularioEditar(@PathVariable Integer codigo, Model model) {
        Cliente cliente = clienteService.buscarPorId(codigo);

        EditarClienteForm form = new EditarClienteForm();
        form.setCodigo(cliente.getCodigo());
        form.setNombre(cliente.getNombre());
        form.setCorreo(cliente.getCorreo());
        form.setTelefono(cliente.getTelefono() != null ? String.valueOf(cliente.getTelefono()) : "");
        form.setDireccion(cliente.getDireccion());

        model.addAttribute("editarClienteForm", form);
        return "interno/comercial-cliente-form";
    }

    @PostMapping
    public String actualizar(@Valid @ModelAttribute EditarClienteForm editarClienteForm,
                             BindingResult resultado, Model model) {
        if (resultado.hasErrors()) {
            return "interno/comercial-cliente-form";
        }
        try {
            clienteService.actualizar(editarClienteForm);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorNegocio", ex.getMessage());
            return "interno/comercial-cliente-form";
        }
        return "redirect:/comercial/clientes";
    }
}