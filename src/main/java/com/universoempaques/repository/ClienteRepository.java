package com.universoempaques.repository;

import com.universoempaques.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}
