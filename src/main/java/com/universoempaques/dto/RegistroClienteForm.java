package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que llena un cliente al autoregistrarse (RF-02).
 * Las reglas de formato estan en DatosColombia.
 */
@Getter
@Setter
public class RegistroClienteForm {

    @NotBlank(message = "El NIT o documento es obligatorio")
    @Pattern(regexp = DatosColombia.NIT, message = DatosColombia.MSG_NIT)
    private String nit;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = DatosColombia.NOMBRE_EMPRESA, message = DatosColombia.MSG_NOMBRE_EMPRESA)
    private String nombre;

    @Pattern(regexp = DatosColombia.NOMBRE_EMPRESA, message = DatosColombia.MSG_NOMBRE_EMPRESA)
    private String razonSocial;

    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    @Email(regexp = DatosColombia.CORREO, message = DatosColombia.MSG_CORREO)
    private String correo;

    /** Obligatorio: es el medio de contacto para avisarle del pedido. */
    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = DatosColombia.CELULAR, message = DatosColombia.MSG_CELULAR)
    private String celular;

    @Pattern(regexp = DatosColombia.TELEFONO, message = DatosColombia.MSG_TELEFONO)
    private String telefono;

    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccion;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = DatosColombia.CONTRASENA, message = DatosColombia.MSG_CONTRASENA)
    private String contrasena;

    @NotBlank(message = "Debes confirmar la contraseña")
    private String confirmarContrasena;
}
