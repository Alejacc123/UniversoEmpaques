package com.universoempaques.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.beans.PropertyEditorSupport;

/**
 * Se aplica a TODOS los formularios de la aplicacion antes de validar:
 *   - Quita espacios al inicio y al final de cada campo de texto
 *     (el autocompletar del celular suele agregar un espacio al final).
 *   - Un campo vacio llega como null, asi los campos opcionales no
 *     fallan las validaciones de formato.
 *   - Las contrasenas NO se tocan: un espacio puede ser parte de ella.
 *   - Si alguien sube un archivo gigante (mas de 5 MB), vuelve a la
 *     pagina anterior con un mensaje en vez de una pagina de error.
 */
@ControllerAdvice
public class FormulariosAdvice {

    @InitBinder
    public void configurar(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

        PropertyEditorSupport sinCambios = new PropertyEditorSupport() {
            @Override
            public void setAsText(String texto) {
                setValue(texto);
            }
        };
        binder.registerCustomEditor(String.class, "contrasena", sinCambios);
        binder.registerCustomEditor(String.class, "confirmarContrasena", sinCambios);
        binder.registerCustomEditor(String.class, "contrasenaActual", sinCambios);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoMuyGrande(HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "El archivo es demasiado grande. El máximo para un diseño es 64 KB.");
        // Solo se usa la RUTA de la pagina anterior (nunca otro dominio)
        String destino = "/mi-panel";
        try {
            String anterior = request.getHeader("Referer");
            if (anterior != null) {
                String ruta = java.net.URI.create(anterior).getPath();
                if (ruta != null && ruta.startsWith("/") && !ruta.startsWith("//")) destino = ruta;
            }
        } catch (IllegalArgumentException ignorado) {
            // Referer raro: se vuelve al panel
        }
        return "redirect:" + destino;
    }
}
