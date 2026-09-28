package com.universoempaques.repository;

import com.universoempaques.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByNumDocumento(String numDocumento);
    List<Usuario> findAllByOrderByNombreAsc();
}
