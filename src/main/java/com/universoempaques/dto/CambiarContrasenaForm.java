package com.universoempaques.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * "Mi cuenta": cualquier persona que inicio sesion cambia su propia
 * contrasena. Pide la actual para que nadie la cambie con una sesion
 * abierta ajena. La nueva cumple la misma regla del registro.
 */
@Getter
@Setter
public class CambiarContrasenaForm {

    @NotBlank(message = "Escribe tu contraseña actual")
    private String contrasenaActual;

    @NotBlank(message = "Escribe la contraseña nueva")
    @Pattern(regexp = DatosColombia.CONTRASENA, message = DatosColombia.MSG_CONTRASENA)
    private String contrasena;

    @NotBlank(message = "Confirma la contraseña nueva")
    private String confirmarContrasena;
}
