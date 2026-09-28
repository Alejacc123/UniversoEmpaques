package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Detalle de un pedido (tabla "DetallePedido"): que producto, cuantas
 * unidades y a que precio.
 *
 * precioUnitario es una "foto" del precio del producto en el momento
 * en que se agrego al pedido. Si despues cambia el precio de lista,
 * este valor NO cambia (trazabilidad, nota del modelo ER).
 */
@Entity
@Table(name = "DetallePedido")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Column(name = "Cantidad")
    private Integer cantidad;

    @ManyToOne
    @JoinColumn(name = "CodigoPedido")
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "CodigoProducto")
    private Producto producto;

    @Column(name = "PrecioUnitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /** Cantidad x precio unitario (no se guarda en la BD). */
    @Transient
    public BigDecimal getSubtotal() {
        if (cantidad == null || precioUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
