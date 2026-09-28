package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pedido de un cliente, atendido por un usuario interno (tabla "Pedido").
 * RF-10, RF-11, RF-14, RF-15.
 */
@Entity
@Table(name = "Pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    /**
     * Fecha en que se registro el pedido. En el script la columna se
     * llama "FechaRegistroTecnicas" (pendiente con Amelie: el
     * diccionario dice "FechaRegistro").
     */
    @Column(name = "FechaRegistroTecnicas")
    private LocalDateTime fechaRegistro;

    @Column(name = "FechaEntrega")
    private LocalDateTime fechaEntrega;

    /** FK a Cliente por NIT (texto). */
    @ManyToOne
    @JoinColumn(name = "NITCliente")
    private Cliente cliente;

    /** Usuario interno que registro/atiende el pedido. */
    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    @Column(name = "FormaPago")
    private String formaPago;

    @Column(name = "DireccionEntrega")
    private String direccionEntrega;
}
