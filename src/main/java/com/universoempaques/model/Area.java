package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Area/cargo de la empresa (Comercial, Diseno, Produccion, Bodega).
 * En el modelo v2 de Amelie la columna se llama "Nombre" (antes "Tipo").
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

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "direccion")
    private String direccion;

    public Area(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
    }
}