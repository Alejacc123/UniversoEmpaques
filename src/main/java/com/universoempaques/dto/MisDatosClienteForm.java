package com.universoempaques.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * "Mi cuenta" del cliente: los datos de contacto que el mismo cliente
 * puede actualizar. El NIT, el nombre y la razon social NO se cambian
 * aqui (identifican a la empresa); eso lo hace Comercial (RF-05).
 */
@Getter
@Setter
public class MisDatosClienteForm {

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
}
