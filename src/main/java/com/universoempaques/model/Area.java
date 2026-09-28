package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Area/cargo de la empresa (tabla "Area"): Comercial, Diseno,
 * Produccion, Bodega. Define a que panel entra el empleado.
 */
@Entity
@Table(name = "Area")
@Getter
@Setter
@NoArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "Nombre")
    private String nombre;

    @Column(name = "Direccion")
    private String direccion;

    /**
     * Nombre para mostrar en pantalla, con tildes. En la BD se guarda
     * sin tildes ("Diseno", "Produccion") porque asi se arma el permiso
     * ROLE_DISENO / ROLE_PRODUCCION (ver AppUserPrincipal).
     */
    @Transient
    public String getNombreVisible() {
        if (nombre == null) return null;
        return switch (nombre) {
            case "Diseno" -> "Diseño";
            case "Produccion" -> "Producción";
            default -> nombre;
        };
    }

    public Area(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
    }
}
