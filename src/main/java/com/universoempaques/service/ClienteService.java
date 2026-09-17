package com.universoempaques.service;

import com.universoempaques.dto.EditarClienteForm;
import com.universoempaques.dto.RegistroClienteForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.repository.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * RF-02: registro de clientes (autoregistro), con acceso inmediato,
     * sin aprobacion previa.
     */
    public Cliente registrar(RegistroClienteForm form) {
        if (clienteRepository.existsByCorreo(form.getCorreo())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }
        if (!form.getContrasena().equals(form.getConfirmarContrasena())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(form.getNombre());
        cliente.setCorreo(form.getCorreo());
        cliente.setDireccion(form.getDireccion());
        if (form.getTelefono() != null && !form.getTelefono().isBlank()) {
            cliente.setTelefono(Integer.parseInt(form.getTelefono().replaceAll("\\D", "")));
        }
        cliente.setContrasena(passwordEncoder.encode(form.getContrasena()));

        return clienteRepository.save(cliente);
    }

    /**
     * RF-05: el area comercial consulta y administra las cuentas
     * de los clientes registrados.
     */
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Integer codigo) {
        return clienteRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
    }

    public Cliente actualizar(EditarClienteForm form) {
        Cliente cliente = buscarPorId(form.getCodigo());

        if (!cliente.getCorreo().equals(form.getCorreo())
                && clienteRepository.existsByCorreo(form.getCorreo())) {
            throw new IllegalArgumentException("Ya existe otro cliente con ese correo.");
        }

        cliente.setNombre(form.getNombre());
        cliente.setCorreo(form.getCorreo());
        cliente.setDireccion(form.getDireccion());
        if (form.getTelefono() != null && !form.getTelefono().isBlank()) {
            cliente.setTelefono(Integer.parseInt(form.getTelefono().replaceAll("\\D", "")));
        } else {
            cliente.setTelefono(null);
        }

        return clienteRepository.save(cliente);
    }
}