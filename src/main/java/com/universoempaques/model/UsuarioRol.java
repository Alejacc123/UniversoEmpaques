package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla puente "UsuarioRol": que rol(es) tiene cada usuario.
 * Se crea y se borra desde Usuario (cascade), no tiene repositorio propio.
 */
@Entity
@Table(name = "UsuarioRol")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "CodigoRol")
    private Rol rol;

    public UsuarioRol(Usuario usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
    }
}
