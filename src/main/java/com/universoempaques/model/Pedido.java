package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pedido realizado por un cliente y atendido por un usuario interno
 * (RF-10, RF-11, RF-14, RF-15).
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @ManyToOne
    @JoinColumn(name = "codigo_cliente")
    private Cliente cliente;

    /** Usuario interno (area) al que esta asignado actualmente el pedido. */
    @ManyToOne
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;
}
