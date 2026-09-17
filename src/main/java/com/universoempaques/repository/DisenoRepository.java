package com.universoempaques.repository;

import com.universoempaques.model.Diseno;
import com.universoempaques.model.EstadoDisenoTipo;
import com.universoempaques.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisenoRepository extends JpaRepository<Diseno, Integer> {
    Optional<Diseno> findByPedido(Pedido pedido);
    List<Diseno> findByEstado(EstadoDisenoTipo estado);
}
