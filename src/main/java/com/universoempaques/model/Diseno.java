package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Diseno de un producto dentro de un pedido (tabla "Diseno").
 * En el modelo v2 cuelga de DetallePedido (cada producto del pedido
 * puede tener su propio diseno y varias versiones). RF-12, RF-13.
 */
@Entity
@Table(name = "Diseno")
@Getter
@Setter
@NoArgsConstructor
public class Diseno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    /** Texto en la BD (ver comentario en EstadoPedido.estado). */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "Estado")
    private EstadoDisenoTipo estado;

    /** Color en formato RGB, ej: "rgb(75,147,182)" (nota del modelo ER). */
    @Column(name = "Color")
    private String color;

    /**
     * Archivos binarios. columnDefinition = "MEDIUMBLOB" hace que "validate"
     * acepte la columna del script (sin esto Hibernate espera otro tipo).
     * MEDIUMBLOB guarda hasta 16 MB; la app limita cada archivo a 5 MB.
     */
    @Lob
    @Column(name = "Logo", columnDefinition = "MEDIUMBLOB")
    private byte[] logo;

    @Lob
    @Column(name = "ArchivoDiseno", columnDefinition = "MEDIUMBLOB")
    private byte[] archivoDiseno;

    @Column(name = "FechaAprobado")
    private LocalDate fechaAprobado;

    @Column(name = "Observaciones")
    private String observaciones;

    /** Usuario de Diseno que elabora/revisa. */
    @ManyToOne
    @JoinColumn(name = "CodigoUsuario")
    private Usuario usuario;

    @Column(name = "Version")
    private Integer version;

    @ManyToOne
    @JoinColumn(name = "CodigoDetallePedido")
    private DetallePedido detallePedido;

    /** ¿El archivo es una imagen (PNG/JPG)? Si no, es PDF. Sirve para mostrar la miniatura. */
    @Transient
    public boolean isArchivoImagen() {
        byte[] b = archivoDiseno;
        return b != null && b.length > 3 && ((b[0] & 0xFF) == 0x89 || (b[0] & 0xFF) == 0xFF);
    }
}
