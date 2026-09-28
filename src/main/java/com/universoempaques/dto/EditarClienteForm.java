package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que el area comercial puede actualizar de un cliente (RF-05).
 * El NIT NO se edita (es la llave primaria). No incluye contrasena.
 */
@Getter
@Setter
public class EditarClienteForm {

    /** Solo lectura en el formulario (viaja oculto). */
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

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = DatosColombia.CELULAR, message = DatosColombia.MSG_CELULAR)
    private String celular;

    @Pattern(regexp = DatosColombia.TELEFONO, message = DatosColombia.MSG_TELEFONO)
    private String telefono;

    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccion;

    /** ACTIVO / INACTIVO. Un cliente inactivo no puede iniciar sesion. */
    @NotBlank(message = "Selecciona el estado")
    @Pattern(regexp = "ACTIVO|INACTIVO", message = "Estado inválido")
    private String estadoCliente;
}
