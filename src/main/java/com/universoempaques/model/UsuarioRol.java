package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla puente: relacion muchos-a-muchos entre Usuario y Rol,
 * tal como la diseño Amelie en el modelo v2.
 */
@Entity
@Table(name = "usuario_rol")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @ManyToOne
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "codigo_rol")
    private Rol rol;
}