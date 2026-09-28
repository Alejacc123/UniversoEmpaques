package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario: trabajador interno de Universo Empaques. En el modelo v2
 * ya NO tiene una unica Area/Rol por columna: puede tener varios,
 * a traves de las tablas puente UsuarioRol y UsuarioArea.
 *
 * Para no complicar la logica de la aplicacion (login, menus, permisos),
 * asumimos que cada usuario, en la practica, tendra UN rol principal y
 * UN area principal (los primeros que se le asignen). La base de datos
 * queda libre para soportar mas adelante multiples roles/areas si el
 * negocio lo llegara a necesitar.
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

    @Column(name = "telefono_fijo")
    private String telefonoFijo;

    @Column(name = "telefono_celular")
    private String telefonoCelular;

    @Column(name = "num_documento")
    private String numDocumento;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "cant_horas_trabajadas")
    private BigDecimal cantHorasTrabajadas;

    @Column(name = "fec_vencimiento_contrato")
    private LocalDate fecVencimientoContrato;

    @Column(name = "nomina")
    private BigDecimal nomina;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioRol> usuarioRoles = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioArea> usuarioAreas = new HashSet<>();

    /** Rol principal del usuario (el primero que se le asigno). */
    @Transient
    public Rol getRolPrincipal() {
        return usuarioRoles.stream().findFirst().map(UsuarioRol::getRol).orElse(null);
    }

    /** Area/cargo principal del usuario (el primero que se le asigno). */
    @Transient
    public Area getAreaPrincipal() {
        return usuarioAreas.stream().findFirst().map(UsuarioArea::getArea).orElse(null);
    }

    @Transient
    public boolean esAdministrador() {
        Rol rol = getRolPrincipal();
        return rol != null && Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
    }
}