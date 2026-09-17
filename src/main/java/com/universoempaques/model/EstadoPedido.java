package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro historico de cada etapa por la que pasa un pedido
 * (RF-14: actualizar estado, RF-15: consultar estado).
 * Cada fila es "una etapa" (ej: en produccion, despachado, etc.)
 * con su fecha de inicio y fin.
 */
@Entity
@Table(name = "estado_pedido")
@Getter
@Setter
@NoArgsConstructor
public class EstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoPedidoTipo estado;

    /** Observacion u novedad registrada por el usuario en esta etapa. */
    @Column(name = "reporte")
    private String reporte;

    /** Usuario que registro/actualizo esta etapa. */
    @ManyToOne
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "codigo_pedido")
    private Pedido pedido;
}
