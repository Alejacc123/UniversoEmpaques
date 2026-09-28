package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Usuario: trabajador interno de Universo Empaques (tabla "Usuario").
 *
 * En el modelo v2 ya no tiene columnas de Rol/Area: se relaciona con
 * ellos por las tablas puente UsuarioRol y UsuarioArea. La base de
 * datos permite varios, pero la aplicacion usa en la practica UN rol
 * principal y UN area principal: el primero que se le asigno (el de
 * menor Codigo en la tabla puente, gracias a @OrderBy).
 *
 * Las dos colecciones son EAGER porque el login (AppUserPrincipal) y
 * las vistas las leen fuera de una transaccion (open-in-view=false).
 * Son Set (no List) para que Hibernate pueda cargar las dos a la vez.
 */
@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "Correo")
    private String correo;

    @Column(name = "Contrasena")
    private String contrasena;

    /**
     * Nombre del trabajador. En el script la columna se llama
     * "NombreEmpresa" (pendiente con Amelie: el diccionario dice "Nombre").
     */
    @Column(name = "NombreEmpresa")
    private String nombre;

    @Column(name = "TelefonoPersonal")
    private String telefonoPersonal;

    /** Solo digitos. VARCHAR(20) en schema.sql (como INT no cabian 10 digitos). */
    @Column(name = "TelefonoEmpresa")
    private String telefonoEmpresa;

    @Column(name = "NumDocumento")
    private String numDocumento;

    @Column(name = "FechaIngreso")
    private LocalDate fechaIngreso;

    @Column(name = "CantHorasTrabajadas", precision = 10, scale = 2)
    private BigDecimal cantHorasTrabajadas;

    @Column(name = "FecVencimientoContrato")
    private LocalDate fecVencimientoContrato;

    @Column(name = "Nomina", precision = 10, scale = 2)
    private BigDecimal nomina;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("codigo ASC")
    private Set<UsuarioRol> usuarioRoles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("codigo ASC")
    private Set<UsuarioArea> usuarioAreas = new LinkedHashSet<>();

    /** Rol principal: el primero que se le asigno. */
    @Transient
    public Rol getRolPrincipal() {
        return usuarioRoles.stream().findFirst().map(UsuarioRol::getRol).orElse(null);
    }

    /** Area principal: la primera que se le asigno. */
    @Transient
    public Area getAreaPrincipal() {
        return usuarioAreas.stream().findFirst().map(UsuarioArea::getArea).orElse(null);
    }

    /**
     * Deja al usuario con un solo rol. Si ya tenia ese mismo rol como
     * principal, no toca nada (asi no se borra y recrea la fila).
     */
    public void asignarRolPrincipal(Rol rol) {
        Rol actual = getRolPrincipal();
        if (actual != null && rol != null && actual.getCodigo().equals(rol.getCodigo())
                && usuarioRoles.size() == 1) {
            return;
        }
        usuarioRoles.clear();
        if (rol != null) {
            usuarioRoles.add(new UsuarioRol(this, rol));
        }
    }

    /** Igual que asignarRolPrincipal, pero con el area. null = sin area. */
    public void asignarAreaPrincipal(Area area) {
        Area actual = getAreaPrincipal();
        if (actual != null && area != null && actual.getCodigo().equals(area.getCodigo())
                && usuarioAreas.size() == 1) {
            return;
        }
        usuarioAreas.clear();
        if (area != null) {
            usuarioAreas.add(new UsuarioArea(this, area));
        }
    }

    @Transient
    public boolean esAdministrador() {
        Rol rol = getRolPrincipal();
        return rol != null && Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
    }
}
