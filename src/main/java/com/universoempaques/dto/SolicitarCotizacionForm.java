package com.universoempaques.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que llena el CLIENTE para pedir una cotizacion (RF-06).
 *
 * La tabla Cotizacion solo tiene una columna de texto para esto
 * ("EspecificacionesEmpaqueSolicitado", VARCHAR(255)), asi que
 * CotizacionService arma un solo texto con estos campos. Los limites
 * de tamano garantizan que ese texto nunca pase de 255 caracteres.
 */
@Getter
@Setter
public class SolicitarCotizacionForm {

    /** Nombre de un producto del catalogo, o "Otro". */
    @NotBlank(message = "Selecciona el tipo de empaque")
    @Size(max = 40, message = "Máximo 40 caracteres")
    private String tipoEmpaque;

    @NotNull(message = "Indica cuántas unidades necesitas")
    @Min(value = 1, message = "Mínimo 1 unidad")
    @Max(value = 1000000, message = "Máximo 1.000.000 unidades")
    private Integer cantidad;

    /** Ej: "30x20x15 cm". */
    @NotBlank(message = "Indica las medidas")
    @Size(max = 30, message = "Máximo 30 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9 .,x×/\"'-]+$", message = "Usa solo números, letras y x (ej: 30x20x15 cm)")
    private String medidas;

    @Size(max = 30, message = "Máximo 30 caracteres")
    private String material;

    @Size(max = 90, message = "Máximo 90 caracteres")
    private String observaciones;
}
