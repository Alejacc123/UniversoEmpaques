package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Cliente: empresa o persona externa que se autoregistra (RF-02).
 * Tabla "Cliente" del script de Amelie. Su llave primaria es el NIT
 * (texto que escribe el cliente), NO un consecutivo automatico.
 *
 * Los nombres de atributo en Java son "limpios"; los nombres raros
 * del script quedan solo dentro de @Column. Si Amelie renombra una
 * columna, solo se cambia la anotacion, no el resto del codigo.
 */
@Entity
@Table(name = "Cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_INACTIVO = "INACTIVO";

    @Id
    @Column(name = "NIT")
    private String nit;

    @Column(name = "Correo")
    private String correo;

    @Column(name = "Contrasena")
    private String contrasena;

    @Column(name = "Direccion")
    private String direccion;

    /** Nombre del cliente o de su empresa (columna "NombreEmpresa"). */
    @Column(name = "NombreEmpresa")
    private String nombre;

    /**
     * Telefono fijo o celular, solo digitos (ej: "6076851234").
     * Es VARCHAR(20) en schema.sql: como INT no cabia un numero de
     * 10 digitos (el maximo de INT es 2.147.483.647).
     */
    @Column(name = "Telefono")
    private String telefono;

    @Column(name = "FechaRegistro")
    private LocalDate fechaRegistro;

    /** ACTIVO / INACTIVO. */
    @Column(name = "EstadoCliente")
    private String estadoCliente;

    @Column(name = "Celular")
    private String celular;

    @Column(name = "RazonSocial")
    private String razonSocial;

    @Transient
    public boolean estaActivo() {
        return !ESTADO_INACTIVO.equalsIgnoreCase(estadoCliente);
    }
}
