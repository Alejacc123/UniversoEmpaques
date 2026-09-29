package com.universoempaques.service;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.CambiarContrasenaForm;
import com.universoempaques.dto.DatosColombia;
import com.universoempaques.dto.MisDatosClienteForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Mi cuenta": lo que cada persona puede cambiar de SU propia cuenta.
 *   - Todos (clientes y trabajadores): su contrasena.
 *   - Clientes: sus datos de contacto (correo, celular, telefono, direccion).
 *
 * Siempre se vuelve a leer la cuenta de la base (no se confia en la copia
 * que guarda la sesion, que puede estar desactualizada).
 */
@Service
public class CuentaService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CuentaService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cambia la contrasena de quien inicio sesion.
     * @throws IllegalArgumentException con un mensaje para mostrar en pantalla
     */
    @Transactional
    public void cambiarContrasena(AppUserPrincipal quien, CambiarContrasenaForm form) {
        if (!form.getContrasena().equals(form.getConfirmarContrasena())) {
            throw new IllegalArgumentException("La contraseña nueva y su confirmación no coinciden.");
        }
        if (form.getContrasena().equals(form.getContrasenaActual())) {
            throw new IllegalArgumentException("La contraseña nueva debe ser distinta a la actual.");
        }

        if (quien.esCliente()) {
            Cliente cliente = clienteActual(quien);
            verificarActual(form.getContrasenaActual(), cliente.getContrasena());
            cliente.setContrasena(passwordEncoder.encode(form.getContrasena()));
            clienteRepository.save(cliente);
        } else {
            Usuario usuario = usuarioRepository.findById(quien.getUsuario().getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Tu cuenta ya no existe."));
            verificarActual(form.getContrasenaActual(), usuario.getContrasena());
            usuario.setContrasena(passwordEncoder.encode(form.getContrasena()));
            usuarioRepository.save(usuario);
        }
    }

    /** Datos actuales del cliente para llenar el formulario. */
    public MisDatosClienteForm datosDelCliente(AppUserPrincipal quien) {
        Cliente cliente = clienteActual(quien);
        MisDatosClienteForm form = new MisDatosClienteForm();
        form.setCorreo(cliente.getCorreo());
        form.setCelular(cliente.getCelular());
        form.setTelefono(cliente.getTelefono());
        form.setDireccion(cliente.getDireccion());
        return form;
    }

    /** Cliente con NIT, nombre y razon social (solo lectura en la pantalla). */
    public Cliente clienteActual(AppUserPrincipal quien) {
        if (!quien.esCliente()) {
            throw new IllegalArgumentException("Esta opción es solo para clientes.");
        }
        return clienteRepository.findById(quien.getCliente().getNit())
                .orElseThrow(() -> new IllegalArgumentException("Tu cuenta ya no existe."));
    }

    /**
     * El cliente actualiza sus datos de contacto.
     * @return true si cambio el correo (hay que volver a iniciar sesion con el nuevo)
     */
    @Transactional
    public boolean actualizarDatosCliente(AppUserPrincipal quien, MisDatosClienteForm form) {
        Cliente cliente = clienteActual(quien);
        String correo = form.getCorreo().trim().toLowerCase();
        boolean cambioCorreo = !correo.equalsIgnoreCase(cliente.getCorreo());
        if (cambioCorreo && (clienteRepository.existsByCorreo(correo) || usuarioRepository.existsByCorreo(correo))) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }
        cliente.setCorreo(correo);
        cliente.setCelular(DatosColombia.soloDigitos(form.getCelular()));
        cliente.setTelefono(DatosColombia.soloDigitos(form.getTelefono()));
        cliente.setDireccion(form.getDireccion() == null || form.getDireccion().isBlank() ? null : form.getDireccion().trim());
        clienteRepository.save(cliente);
        return cambioCorreo;
    }

    private void verificarActual(String escrita, String cifrada) {
        if (cifrada == null || !passwordEncoder.matches(escrita, cifrada)) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
    }
}
