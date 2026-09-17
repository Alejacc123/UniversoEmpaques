package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que llena el administrador para registrar un trabajador
 * (RF-03) y asignarle su cargo (area) y su nivel de permisos (rol),
 * que es justamente lo que implementa RF-04.
 *
 * La contrasena es opcional: si se deja en blanco al EDITAR un
 * usuario existente, se conserva la que ya tenia.
 */
@Getter
@Setter
public class RegistrarUsuarioForm {

    private Integer codigo; // null = usuario nuevo; con valor = edicion

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo valido")
    private String correo;

    private String telefono;

    private String contrasena;

    @NotNull(message = "Debes seleccionar un rol")
    private Integer codigoRol;

    // El area es opcional: un Administrador no necesariamente
    // pertenece a un area operativa (Comercial, Diseno, etc.)
    private Integer codigoArea;
}