package com.universoempaques.service;

import com.universoempaques.dto.DatosColombia;
import com.universoempaques.dto.EditarClienteForm;
import com.universoempaques.dto.RegistroClienteForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Logica de clientes (patron Facade: el controlador no toca los
 * repositorios directamente). RF-02 y RF-05.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * RF-02: autoregistro del cliente, con acceso inmediato (sin aprobacion).
     *
     * OJO: como el NIT lo escribe el usuario (no es autoincremental),
     * save() con un NIT que ya existe NO falla: SOBRESCRIBE al cliente
     * anterior. Por eso se valida antes con existsById.
     */
    @Transactional
    public Cliente registrar(RegistroClienteForm form) {
        String nit = form.getNit().trim();
        String correo = form.getCorreo().trim().toLowerCase();

        if (clienteRepository.existsById(nit)) {
            throw new IllegalArgumentException("Ya existe un cliente registrado con ese NIT.");
        }
        validarCorreoLibre(correo);
        if (!form.getContrasena().equals(form.getConfirmarContrasena())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        Cliente cliente = new Cliente();
        cliente.setNit(nit);
        cliente.setNombre(form.getNombre().trim());
        cliente.setRazonSocial(vacioANull(form.getRazonSocial()));
        cliente.setCorreo(correo);
        cliente.setTelefono(DatosColombia.soloDigitos(form.getTelefono()));
        cliente.setCelular(DatosColombia.soloDigitos(form.getCelular()));
        cliente.setDireccion(vacioANull(form.getDireccion()));
        cliente.setContrasena(passwordEncoder.encode(form.getContrasena()));
        cliente.setFechaRegistro(LocalDate.now());
        cliente.setEstadoCliente(Cliente.ESTADO_ACTIVO);

        return clienteRepository.save(cliente);
    }

    /** RF-05: el area comercial consulta los clientes registrados. */
    public List<Cliente> listarTodos() {
        return clienteRepository.findAllByOrderByNombreAsc();
    }

    public Cliente buscarPorNit(String nit) {
        return clienteRepository.findById(nit)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
    }

    /** RF-05: el area comercial actualiza los datos del cliente (menos el NIT). */
    @Transactional
    public Cliente actualizar(EditarClienteForm form) {
        Cliente cliente = buscarPorNit(form.getNit());
        String correo = form.getCorreo().trim().toLowerCase();

        if (!correo.equalsIgnoreCase(cliente.getCorreo())) {
            validarCorreoLibre(correo);
        }

        cliente.setNombre(form.getNombre().trim());
        cliente.setRazonSocial(vacioANull(form.getRazonSocial()));
        cliente.setCorreo(correo);
        cliente.setTelefono(DatosColombia.soloDigitos(form.getTelefono()));
        cliente.setCelular(DatosColombia.soloDigitos(form.getCelular()));
        cliente.setDireccion(vacioANull(form.getDireccion()));
        cliente.setEstadoCliente(form.getEstadoCliente());

        return clienteRepository.save(cliente);
    }

    /**
     * El login es uno solo para clientes y trabajadores, asi que un
     * correo no puede repetirse en NINGUNA de las dos tablas.
     */
    private void validarCorreoLibre(String correo) {
        if (clienteRepository.existsByCorreo(correo) || usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }
    }

    private static String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
