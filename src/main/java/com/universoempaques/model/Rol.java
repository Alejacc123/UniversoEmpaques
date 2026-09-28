package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rol (tabla "Rol"): nivel jerarquico de permisos del trabajador
 * (Administrador o Empleado). Junto con el Area define que puede
 * hacer cada quien en el sistema (RF-04).
 */
@Entity
@Table(name = "Rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String EMPLEADO = "Empleado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "Nombre")
    private String nombre;

    /** Descripcion en texto de lo que puede hacer el rol (informativo). */
    @Column(name = "Permisos")
    private String permisos;

    public Rol(String nombre, String permisos) {
        this.nombre = nombre;
        this.permisos = permisos;
    }
}
