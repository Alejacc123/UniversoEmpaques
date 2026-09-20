package com.universoempaques.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Cliente: persona o empresa externa que se autoregistra en el
 * sistema (RF-02). Su identificador es el NIT, no un consecutivo.
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @Column(name = "nit", length = 255)
    private String nit;

    @Column(name = "correo", unique = true)
    private String correo;

    @Column(name = "contrasena")
    private String contrasena;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    /** Estado actual del cliente: activo / inactivo. */
    @Column(name = "estado_cliente")
    private String estadoCliente;

    @Column(name = "celular")
    private String celular;

    @Column(name = "razon_social")
    private String razonSocial;
}