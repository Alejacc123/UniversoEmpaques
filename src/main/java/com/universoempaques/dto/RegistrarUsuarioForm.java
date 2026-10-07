package com.universoempaques.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que llena el administrador para registrar/editar un trabajador
 * (RF-03) y asignarle rol y area (RF-04).
 *
 * La contrasena es opcional al EDITAR: si se deja en blanco, se
 * conserva la que ya tenia (la obligatoriedad al crear la revisa
 * UsuarioService).
 */
@Getter
@Setter
public class RegistrarUsuarioForm {

    private Integer codigo; // null = usuario nuevo; con valor = edicion

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = DatosColombia.NOMBRE_PERSONA, message = DatosColombia.MSG_NOMBRE_PERSONA)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    @Email(regexp = DatosColombia.CORREO, message = DatosColombia.MSG_CORREO)
    private String correo;

    @Pattern(regexp = DatosColombia.CONTRASENA_OPCIONAL, message = DatosColombia.MSG_CONTRASENA)
    private String contrasena;

    @Pattern(regexp = DatosColombia.DOCUMENTO, message = DatosColombia.MSG_DOCUMENTO)
    private String numDocumento;

    @Pattern(regexp = DatosColombia.CELULAR, message = DatosColombia.MSG_CELULAR)
    private String telefonoPersonal;

    @Pattern(regexp = DatosColombia.TELEFONO, message = DatosColombia.MSG_TELEFONO)
    private String telefonoEmpresa;

    @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecVencimientoContrato;

    /** Columnas DECIMAL(10,2) en la BD: se limita a valores razonables. */
    @PositiveOrZero(message = "No puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Máximo 99.999.999 horas, con 2 decimales")
    private BigDecimal cantHorasTrabajadas;

    @PositiveOrZero(message = "No puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Máximo 99.999.999, con 2 decimales")
    private BigDecimal nomina;

    @NotNull(message = "Debes seleccionar un rol")
    private Integer codigoRol;

    /** Obligatoria para Empleado (lo revisa UsuarioService); opcional para Administrador. */
    private Integer codigoArea;
}
