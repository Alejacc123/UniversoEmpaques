package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que el area comercial puede consultar y actualizar de un
 * cliente ya registrado (RF-05). No incluye contrasena: eso es algo
 * que solo el propio cliente deberia poder cambiar, no el comercial.
 */
@Getter
@Setter
public class EditarClienteForm {

    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo valido")
    private String correo;

    private String telefono;

    private String direccion;
}