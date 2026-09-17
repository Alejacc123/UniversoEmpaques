package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Define el nivel jerarquico de permisos de un usuario interno
 * (Administrador o Empleado). Junto con Area, determina que puede
 * hacer cada trabajador dentro del sistema (RF-04).
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String EMPLEADO = "Empleado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "nombre")
    private String nombre;

    public Rol(String nombre) {
        this.nombre = nombre;
    }
}
