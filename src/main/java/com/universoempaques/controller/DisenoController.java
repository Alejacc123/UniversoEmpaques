package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.model.Diseno;
import com.universoempaques.service.DisenoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * RF-12: Diseno sube el diseno de cada producto (/diseno/..., solo DISENO y ADMIN).
 * RF-13: Comercial lo aprueba o pide ajustes (/comercial/..., solo COMERCIAL y ADMIN).
 * Ver los archivos (/disenos/...): usuarios internos y el cliente dueno del pedido.
 */
@Controller
public class DisenoController {

    private final DisenoService disenoService;

    public DisenoController(DisenoService disenoService) {
        this.disenoService = disenoService;
    }

    @PostMapping("/diseno/pedidos/{codigoPedido}/disenos")
    public String subir(@PathVariable Integer codigoPedido,
                        @RequestParam Integer codigoDetalle,
                        @RequestParam(required = false) MultipartFile archivo,
                        @RequestParam(required = false) MultipartFile logo,
                        @RequestParam(required = false) String color,
                        @RequestParam(required = false) String observaciones,
                        Authentication auth, RedirectAttributes flash) {
        try {
            Diseno d = disenoService.subir(codigoPedido, codigoDetalle, archivo, logo, color, observaciones,
                    ((AppUserPrincipal) auth.getPrincipal()).getUsuario());
            flash.addFlashAttribute("exito", "Versión " + d.getVersion() + " cargada. Queda pendiente de revisión por Comercial.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            flash.addFlashAttribute("error", "No se pudo leer el archivo: " + ex.getMessage());
        }
        return "redirect:/diseno/pedidos/" + codigoPedido;
    }

    @PostMapping("/comercial/pedidos/{codigoPedido}/disenos/{codigoDiseno}/revisar")
    public String revisar(@PathVariable Integer codigoPedido, @PathVariable Integer codigoDiseno,
                          @RequestParam String decision,
                          @RequestParam(required = false) String comentario,
                          Authentication auth, RedirectAttributes flash) {
        try {
            boolean aprobar = "aprobar".equals(decision);
            disenoService.revisar(codigoPedido, codigoDiseno, aprobar, comentario,
                    ((AppUserPrincipal) auth.getPrincipal()).getUsuario());
            flash.addFlashAttribute("exito", aprobar ? "Diseño aprobado." : "Se pidieron ajustes a Diseño.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/comercial/pedidos/" + codigoPedido;
    }

    /** Muestra el archivo del diseno (o su logo) en el navegador. */
    @GetMapping("/disenos/{codigo}/{que:archivo|logo}")
    public ResponseEntity<byte[]> ver(@PathVariable Integer codigo, @PathVariable String que, Authentication auth) {
        Diseno diseno;
        try {
            diseno = disenoService.buscar(codigo);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
        // Un cliente solo puede ver los disenos de SUS pedidos
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        if (principal.esCliente()) {
            var cliente = diseno.getDetallePedido().getPedido().getCliente();
            if (cliente == null || !cliente.getNit().equals(principal.getCliente().getNit())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        byte[] bytes = "logo".equals(que) ? diseno.getLogo() : diseno.getArchivoDiseno();
        String tipo = DisenoService.tipoDeContenido(bytes);
        if (bytes == null || tipo == null) {
            return ResponseEntity.notFound().build();
        }
        String extension = tipo.equals("application/pdf") ? ".pdf" : tipo.equals("image/png") ? ".png" : ".jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("diseno-" + codigo + "-v" + diseno.getVersion() + ("logo".equals(que) ? "-logo" : "") + extension)
                        .build().toString())
                .body(bytes);
    }
}
