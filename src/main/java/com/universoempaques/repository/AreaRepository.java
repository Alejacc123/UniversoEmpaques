package com.universoempaques.repository;

import com.universoempaques.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    Optional<Area> findByNombre(String nombre);
}