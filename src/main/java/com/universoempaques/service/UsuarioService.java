package com.universoempaques.service;

import com.universoempaques.dto.RegistrarUsuarioForm;
import com.universoempaques.model.Area;
import com.universoempaques.model.Rol;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.AreaRepository;
import com.universoempaques.repository.RolRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, AreaRepository areaRepository,
                          RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Area> listarAreas() {
        return areaRepository.findAll();
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    /**
     * RF-03 (registrar trabajador) y RF-04 (asignar permisos segun
     * cargo): un mismo formulario cubre ambos requisitos, ya que el
     * cargo de un usuario (area + rol) es lo que determina sus
     * permisos en el sistema (ver AppUserPrincipal).
     */
    public Usuario guardar(RegistrarUsuarioForm form) {
        Usuario usuario;

        if (form.getCodigo() != null) {
            usuario = usuarioRepository.findById(form.getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
            if (!usuario.getCorreo().equals(form.getCorreo())
                    && usuarioRepository.existsByCorreo(form.getCorreo())) {
                throw new IllegalArgumentException("Ya existe otro usuario con ese correo.");
            }
        } else {
            if (usuarioRepository.existsByCorreo(form.getCorreo())) {
                throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
            }
            usuario = new Usuario();
        }

        usuario.setNombre(form.getNombre());
        usuario.setCorreo(form.getCorreo());
        if (form.getTelefono() != null && !form.getTelefono().isBlank()) {
            usuario.setTelefono(Integer.parseInt(form.getTelefono().replaceAll("\\D", "")));
        }

        if (form.getContrasena() != null && !form.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(form.getContrasena()));
        } else if (usuario.getContrasena() == null) {
            throw new IllegalArgumentException("Debes definir una contrasena para el nuevo usuario.");
        }

        Rol rol = rolRepository.findById(form.getCodigoRol())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
        usuario.setRol(rol);

        if (form.getCodigoArea() != null) {
            Area area = areaRepository.findById(form.getCodigoArea())
                    .orElseThrow(() -> new IllegalArgumentException("El area seleccionada no existe."));
            usuario.setArea(area);
        } else {
            usuario.setArea(null);
        }

        return usuarioRepository.save(usuario);
    }
}