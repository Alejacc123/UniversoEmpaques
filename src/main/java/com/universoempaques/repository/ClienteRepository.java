package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * OJO: la llave primaria de Cliente ahora es el NIT (String),
 * no un consecutivo entero. Por eso el segundo tipo generico
 * cambio de Integer a String.
 */
public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}