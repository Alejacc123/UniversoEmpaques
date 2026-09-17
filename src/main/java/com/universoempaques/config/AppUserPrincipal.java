package com.universoempaques.config;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Rol;
import com.universoempaques.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;

/**
 * Representa a la persona autenticada, sin importar si es un Cliente
 * o un Usuario interno (trabajador). El login es el mismo formulario
 * para todos (RF-01); esta clase es la que traduce "quien inicio
 * sesion" a los permisos que Spring Security entiende.
 *
 * Autoridades que se generan:
 *   - ROLE_CLIENTE            -> si es un Cliente
 *   - ROLE_ADMIN              -> si es Usuario con Rol = Administrador
 *   - ROLE_<AREA>             -> si es Usuario con Rol = Empleado
 *                                (ej: ROLE_COMERCIAL, ROLE_DISENO,
 *                                ROLE_PRODUCCION, ROLE_BODEGA)
 */
public class AppUserPrincipal implements UserDetails {

    private final String correo;
    private final String contrasenaCifrada;
    private final String nombre;
    private final List<GrantedAuthority> authorities;

    private final Cliente cliente;   // != null si quien inicio sesion es un cliente
    private final Usuario usuario;   // != null si quien inicio sesion es un trabajador

    public static AppUserPrincipal deCliente(Cliente cliente) {
        return new AppUserPrincipal(
                cliente.getCorreo(),
                cliente.getContrasena(),
                cliente.getNombre(),
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")),
                cliente,
                null
        );
    }

    public static AppUserPrincipal deUsuario(Usuario usuario) {
        String autoridad;
        if (usuario.esAdministrador()) {
            autoridad = "ROLE_ADMIN";
        } else {
            String area = usuario.getArea() != null ? usuario.getArea().getTipo() : "EMPLEADO";
            autoridad = "ROLE_" + normalizar(area);
        }
        return new AppUserPrincipal(
                usuario.getCorreo(),
                usuario.getContrasena(),
                usuario.getNombre(),
                List.of(new SimpleGrantedAuthority(autoridad)),
                null,
                usuario
        );
    }

    private AppUserPrincipal(String correo, String contrasenaCifrada, String nombre,
                              List<GrantedAuthority> authorities, Cliente cliente, Usuario usuario) {
        this.correo = correo;
        this.contrasenaCifrada = contrasenaCifrada;
        this.nombre = nombre;
        this.authorities = authorities;
        this.cliente = cliente;
        this.usuario = usuario;
    }

    /** Quita tildes y pasa a mayusculas, para que "Diseño" -> "DISENO". */
    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().trim();
    }

    public boolean esCliente() {
        return cliente != null;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombreParaSaludo() {
        return nombre;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return contrasenaCifrada;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
