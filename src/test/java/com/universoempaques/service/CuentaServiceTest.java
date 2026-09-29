package com.universoempaques.service;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.CambiarContrasenaForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** "Mi cuenta": reglas para cambiar la contrasena. */
class CuentaServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // 4 = rapido para pruebas
    private ClienteRepository clientes;
    private CuentaService servicio;
    private Cliente cliente;
    private AppUserPrincipal quien;

    @BeforeEach
    void preparar() {
        clientes = mock(ClienteRepository.class);
        servicio = new CuentaService(clientes, mock(UsuarioRepository.class), encoder);
        cliente = new Cliente();
        cliente.setNit("900000001-1");
        cliente.setCorreo("cliente@prueba.com");
        cliente.setEstadoCliente(Cliente.ESTADO_ACTIVO);
        cliente.setContrasena(encoder.encode("vieja1234"));
        when(clientes.findById("900000001-1")).thenReturn(Optional.of(cliente));
        quien = AppUserPrincipal.deCliente(cliente);
    }

    private CambiarContrasenaForm form(String actual, String nueva, String confirmar) {
        CambiarContrasenaForm f = new CambiarContrasenaForm();
        f.setContrasenaActual(actual);
        f.setContrasena(nueva);
        f.setConfirmarContrasena(confirmar);
        return f;
    }

    @Test
    void cambiaLaContrasenaSiLaActualEsCorrecta() {
        servicio.cambiarContrasena(quien, form("vieja1234", "nueva5678", "nueva5678"));
        assertTrue(encoder.matches("nueva5678", cliente.getContrasena()));
        verify(clientes).save(cliente);
    }

    @Test
    void rechazaSiLaActualEstaMal() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarContrasena(quien, form("otra1234", "nueva5678", "nueva5678")));
        assertTrue(ex.getMessage().contains("actual"));
        verify(clientes, never()).save(any());
    }

    @Test
    void rechazaSiLaConfirmacionNoCoincide() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarContrasena(quien, form("vieja1234", "nueva5678", "nueva0000")));
        verify(clientes, never()).save(any());
    }

    @Test
    void rechazaSiLaNuevaEsIgualALaActual() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarContrasena(quien, form("vieja1234", "vieja1234", "vieja1234")));
    }
}
