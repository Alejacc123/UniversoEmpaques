package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Catalogo de productos/empaques (tabla "Producto").
 * "Precio" es el precio de lista ACTUAL; el precio con el que se
 * vendio en cada pedido queda en DetallePedido.precioUnitario.
 */
@Entity
@Table(name = "Producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "Nombre")
    private String nombre;

    @Column(name = "Material")
    private String material;

    @Column(name = "Precio", precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "Forma")
    private String forma;

    @Column(name = "Tamano")
    private String tamano;
}
