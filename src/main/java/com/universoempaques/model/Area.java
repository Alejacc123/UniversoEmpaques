package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa un area/departamento de la empresa (Comercial, Diseno,
 * Produccion, Bodega). Se usa para categorizar a los usuarios internos
 * (ver clase Usuario).
 */
@Entity
@Table(name = "area")
@Getter
@Setter
@NoArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "direccion")
    private String direccion;

    public Area(String tipo, String direccion) {
        this.tipo = tipo;
        this.direccion = direccion;
    }
}
