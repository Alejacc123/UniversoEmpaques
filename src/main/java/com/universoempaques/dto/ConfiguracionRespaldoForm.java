package com.universoempaques.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Configuracion de la copia automatica que el admin cambia desde la pantalla (RF-19). */
@Getter
@Setter
public class ConfiguracionRespaldoForm {

    private boolean activo;

    /** Hora diaria de la copia, formato 24 h (input type="time"): "19:00". */
    @NotBlank(message = "Indica la hora")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Hora inválida (formato 24 h, ej: 19:00)")
    private String hora;

    /** Segunda carpeta opcional (USB, OneDrive...). Vacio = sin segunda copia. */
    @Size(max = 250, message = "Máximo 250 caracteres")
    private String directorioCopia;
}
