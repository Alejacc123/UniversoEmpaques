package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario: trabajador interno de Universo Empaques (Comercial, Diseno,
 * Produccion, Bodega o Administrador). Se categoriza por Area y su
 * jerarquia de permisos se define con Rol (RF-03, RF-04).
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "correo", unique = true)
    private String correo;

    @Column(name = "contrasena")
    private String contrasena;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "telefono")
    private Integer telefono;

    @ManyToOne
    @JoinColumn(name = "codigo_area")
    private Area area;

    @ManyToOne
    @JoinColumn(name = "codigo_rol")
    private Rol rol;

    /** Atajo para saber si este usuario es administrador (RF-04). */
    @Transient
    public boolean esAdministrador() {
        return rol != null && Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
    }
}
