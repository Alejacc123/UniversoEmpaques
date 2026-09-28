package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * La llave primaria de Cliente es el NIT (String), por eso el segundo
 * tipo generico es String. findById("900123456") busca por NIT.
 */
public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    List<Cliente> findAllByOrderByNombreAsc();
}
