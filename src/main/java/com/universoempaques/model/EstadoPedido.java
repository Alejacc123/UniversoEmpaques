package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Historial de etapas de un pedido (tabla "EstadoPedido").
 * Cada fila es una etapa (SOLICITADO, EN_PRODUCCION, ...) con su
 * fecha de inicio y fin. RF-14 (actualizar) y RF-15 (consultar).
 */
@Entity
@Table(name = "EstadoPedido")
@Getter
@Setter
@NoArgsConstructor
public class EstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "FechaInicio")
    private LocalDateTime fechaInicio;

    @Column(name = "FechaFin")
    private LocalDateTime fechaFin;

    /**
     * Se guarda como texto. @JdbcTypeCode(VARCHAR) es necesario porque
     * Hibernate 6 esperaria una columna tipo ENUM de MySQL y el script
     * usa VARCHAR(255); sin esto, "validate" falla al arrancar.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "Estado")
    private EstadoPedidoTipo estado;

    /** Observacion o novedad registrada en esta etapa. */
    @Column(name = "Reporte")
    private String reporte;

    /** Usuario que registro esta etapa. */
    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "CodigoPedido")
    private Pedido pedido;
}
