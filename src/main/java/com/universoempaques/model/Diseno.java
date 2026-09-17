package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Diseño elaborado para un pedido por el area de Diseño (RF-12, RF-13).
 */
@Entity
@Table(name = "diseno")
@Getter
@Setter
@NoArgsConstructor
public class Diseno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "especificaciones_tecnicas")
    private String especificacionesTecnicas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoDisenoTipo estado;

    @ManyToOne
    @JoinColumn(name = "codigo_pedido")
    private Pedido pedido;

    /** Usuario del area de Diseño que elabora/revisa el diseño. */
    @ManyToOne
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;
}
