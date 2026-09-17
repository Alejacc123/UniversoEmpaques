package com.universoempaques.config;

import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementa RF-01 (inicio de sesion): busca el correo ingresado
 * primero entre los clientes y, si no lo encuentra, entre los
 * usuarios internos (trabajadores). Es lo que permite tener un
 * unico formulario de login para todos los roles.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(ClienteRepository clienteRepository,
                                     UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        // Primero se busca entre los clientes...
        var clienteEncontrado = clienteRepository.findByCorreo(correo);
        if (clienteEncontrado.isPresent()) {
            return AppUserPrincipal.deCliente(clienteEncontrado.get());
        }

        // ...y si no aparece, entre los trabajadores internos.
        var usuarioEncontrado = usuarioRepository.findByCorreo(correo);
        if (usuarioEncontrado.isPresent()) {
            return AppUserPrincipal.deUsuario(usuarioEncontrado.get());
        }

        throw new UsernameNotFoundException(
                "No existe ninguna cuenta registrada con el correo: " + correo);
    }
}
