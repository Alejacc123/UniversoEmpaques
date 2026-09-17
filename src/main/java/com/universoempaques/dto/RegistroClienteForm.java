package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que llena un cliente al autoregistrarse (RF-02).
 * Se valida aqui, antes de convertirlo en la entidad Cliente,
 * para no mezclar reglas de formulario con el modelo de datos.
 */
@Getter
@Setter
public class RegistroClienteForm {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo valido")
    private String correo;

    private String telefono;

    private String direccion;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
    private String contrasena;

    @NotBlank(message = "Debes confirmar la contrasena")
    private String confirmarContrasena;
}
