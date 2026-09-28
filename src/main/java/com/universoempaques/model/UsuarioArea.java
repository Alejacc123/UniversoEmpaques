package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla puente "UsuarioArea": a que area(s) pertenece cada usuario.
 * Se crea y se borra desde Usuario (cascade), no tiene repositorio propio.
 */
@Entity
@Table(name = "UsuarioArea")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "CodigoArea")
    private Area area;

    public UsuarioArea(Usuario usuario, Area area) {
        this.usuario = usuario;
        this.area = area;
    }
}
