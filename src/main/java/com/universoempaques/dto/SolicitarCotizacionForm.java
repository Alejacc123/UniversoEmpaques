package com.universoempaques.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * Datos que llena el CLIENTE para pedir una cotizacion (RF-06).
 *
 * La tabla Cotizacion solo tiene una columna de texto para esto
 * ("EspecificacionesEmpaqueSolicitado", VARCHAR(255)), asi que
 * CotizacionService arma un solo texto con estos campos. Los limites
 * de tamano garantizan que ese texto nunca pase de 255 caracteres.
 *
 * Las medidas son NUMEROS en centimetros (largo, ancho y alto opcional);
 * se guardan juntas como "30x20x15 cm".
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

    @NotNull(message = "Indica el largo")
    @DecimalMin(value = "0.1", message = "Debe ser mayor a 0")
    @DecimalMax(value = "999", message = "Máximo 999 cm")
    @Digits(integer = 3, fraction = 1, message = "Máximo 1 decimal")
    private BigDecimal largo;

    @NotNull(message = "Indica el ancho")
    @DecimalMin(value = "0.1", message = "Debe ser mayor a 0")
    @DecimalMax(value = "999", message = "Máximo 999 cm")
    @Digits(integer = 3, fraction = 1, message = "Máximo 1 decimal")
    private BigDecimal ancho;

    /** Opcional: una bolsa plana no tiene alto. */
    @DecimalMin(value = "0.1", message = "Debe ser mayor a 0")
    @DecimalMax(value = "999", message = "Máximo 999 cm")
    @Digits(integer = 3, fraction = 1, message = "Máximo 1 decimal")
    private BigDecimal alto;

    @Size(max = 30, message = "Máximo 30 caracteres")
    private String material;

    @Size(max = 90, message = "Máximo 90 caracteres")
    private String observaciones;

    /** Foto de referencia opcional (PNG o JPG, hasta 5 MB). La valida CotizacionService. */
    private MultipartFile foto;

    /** "30x20x15 cm" o "25x32 cm" (sin alto). */
    public String getMedidas() {
        StringBuilder m = new StringBuilder(numero(largo)).append("x").append(numero(ancho));
        if (alto != null) {
            m.append("x").append(numero(alto));
        }
        return m.append(" cm").toString();
    }

    private static String numero(BigDecimal valor) {
        if (valor == null) return "?";
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
