package com.universoempaques;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cotizacion solicitada por un cliente (RF-06), registrada por el
 * area comercial (RF-07), aprobada o rechazada por el cliente (RF-08)
 * y consultada por ambos (RF-09).
 */
@Entity
@Table(name = "cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    /** Fecha en que el cliente solicita la cotizacion (RF-06). */
    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "material")
    private String material;

    @Column(name = "medidas")
    private String medidas;

    @Column(name = "cantidad")
    private Integer cantidad;

    /** Caracteristicas adicionales del empaque indicadas por el cliente. */
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    /** Valor registrado por el area comercial (RF-07). */
    @Column(name = "valor_estimado", precision = 12, scale = 2)
    private BigDecimal valorEstimado;

    /** Condiciones de la oferta: tiempos, forma de pago, vigencia, etc. (RF-07). */
    @Column(name = "condiciones", length = 1000)
    private String condiciones;

    /** Fecha en que el area comercial registro valor y condiciones. */
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    /** Fecha en que el cliente aprobo o rechazo la cotizacion (RF-08). */
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoCotizacionTipo estado;

    /** Cliente que solicita la cotizacion (FK al NIT del cliente). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "codigo_cliente", nullable = false)
    private Cliente cliente;

    /** Usuario del area comercial que registra la cotizacion. */
    @ManyToOne
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;

    @PrePersist
    private void alCrear() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoCotizacionTipo.SOLICITADA;
        }
    }

    /** RF-07: el area comercial registra valor y condiciones. */
    public void registrar(BigDecimal valor, String condiciones, Usuario comercial) {
        validarEstado(EstadoCotizacionTipo.SOLICITADA, "registrar");
        this.valorEstimado = valor;
        this.condiciones = condiciones;
        this.usuario = comercial;
        this.fechaRegistro = LocalDateTime.now();
        this.estado = EstadoCotizacionTipo.REGISTRADA;
    }

    /** RF-08: el cliente aprueba la cotizacion. */
    public void aprobar() {
        validarEstado(EstadoCotizacionTipo.REGISTRADA, "aprobar");
        this.fechaRespuesta = LocalDateTime.now();
        this.estado = EstadoCotizacionTipo.APROBADA;
    }

    /** RF-08: el cliente rechaza la cotizacion. */
    public void rechazar() {
        validarEstado(EstadoCotizacionTipo.REGISTRADA, "rechazar");
        this.fechaRespuesta = LocalDateTime.now();
        this.estado = EstadoCotizacionTipo.RECHAZADA;
    }

    private void validarEstado(EstadoCotizacionTipo esperado, String accion) {
        if (this.estado != esperado) {
            throw new IllegalStateException(
                    "No se puede " + accion + " una cotizacion en estado " + this.estado + ".");
        }
    }
}