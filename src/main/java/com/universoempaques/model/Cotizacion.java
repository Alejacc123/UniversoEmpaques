package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Cotizacion (tabla "Cotizacion", nueva en el modelo v2). RF-06 a RF-09.
 *
 * El cliente la solicita (NITCliente), el comercial le pone valor
 * (CodigoUsuario) y, si se aprueba, se enlaza con el Pedido que se
 * genera (CodigoPedido). Por eso "pedido" empieza en null.
 */
@Entity
@Table(name = "Cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    /** DOUBLE en el script. */
    @Column(name = "Valor")
    private Double valor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "Estado")
    private EstadoCotizacionTipo estado;

    @Column(name = "EspecificacionesEmpaqueSolicitado")
    private String especificaciones;

    /** Comercial que elabora la cotizacion. */
    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    /** Pedido generado a partir de esta cotizacion (1:1, "Posee"). */
    @OneToOne
    @JoinColumn(name = "CodigoPedido")
    private Pedido pedido;

    @Column(name = "FechaSolicitud")
    private LocalDate fechaSolicitud;

    @ManyToOne
    @JoinColumn(name = "NITCliente")
    private Cliente cliente;

    /** Valor que propone el cliente en una contraoferta (columna agregada). */
    @Column(name = "ValorContraoferta")
    private Double valorContraoferta;

    /** Comentario del cliente al aprobar, rechazar o contraofertar (columna agregada). */
    @Column(name = "Observaciones")
    private String observaciones;

    /** Foto de referencia opcional que sube el cliente (PNG/JPG, columna agregada). */
    @Lob
    @Column(name = "FotoReferencia", columnDefinition = "MEDIUMBLOB")
    private byte[] fotoReferencia;

    @Transient
    public boolean isTieneFoto() {
        return fotoReferencia != null && fotoReferencia.length > 0;
    }
}
