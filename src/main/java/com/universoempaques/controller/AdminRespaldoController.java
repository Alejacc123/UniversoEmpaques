package com.universoempaques.controller;

import com.universoempaques.dto.ConfiguracionRespaldoForm;
import com.universoempaques.service.BackupService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;

/**
 * Interfaz del administrador para la copia de seguridad (RF-19).
 * La logica del respaldo esta en BackupService (hecho por Amelie);
 * aqui se muestra el estado, se cambia la configuracion, se hace una
 * copia total manual y se descargan los archivos. Solo rol ADMIN.
 */
@Controller
@RequestMapping("/admin/respaldos")
public class AdminRespaldoController {

    private final BackupService backupService;

    public AdminRespaldoController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public String ver(Model model) {
        ConfiguracionRespaldoForm form = new ConfiguracionRespaldoForm();
        form.setActivo(backupService.isActivo());
        form.setHora(backupService.getHoraAutomatica().toString());
        form.setDirectorioCopia(backupService.getDirectorioCopia());
        return mostrar(model, form);
    }

    private String mostrar(Model model, ConfiguracionRespaldoForm form) {
        model.addAttribute("backup", backupService);
        model.addAttribute("archivos", backupService.listarArchivos());
        model.addAttribute("configuracionRespaldoForm", form);
        return "interno/admin-respaldos";
    }

    /** Copia TOTAL en este momento (reemplaza mensual.sql). */
    @PostMapping("/ahora")
    public String respaldarAhora(RedirectAttributes flash) {
        boolean ok = backupService.respaldarTotal();
        flash.addFlashAttribute(ok ? "exito" : "errorCopia", backupService.getUltimoMensaje());
        return "redirect:/admin/respaldos";
    }

    /** Guarda la configuracion de la copia automatica (sin reiniciar la app). */
    @PostMapping("/configuracion")
    public String guardarConfiguracion(@Valid @ModelAttribute ConfiguracionRespaldoForm configuracionRespaldoForm,
                                       BindingResult resultado, Model model, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return mostrar(model, configuracionRespaldoForm);
        }
        try {
            backupService.guardarConfiguracion(configuracionRespaldoForm.isActivo(),
                    LocalTime.parse(configuracionRespaldoForm.getHora()),
                    configuracionRespaldoForm.getDirectorioCopia());
            flash.addFlashAttribute("exito", "Configuración guardada. " + (configuracionRespaldoForm.isActivo()
                    ? backupService.getHorarioLegible() + " se hará la copia automática."
                    : "La copia automática quedó apagada."));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return mostrar(model, configuracionRespaldoForm);
        } catch (IOException ex) {
            model.addAttribute("error", "No se pudo guardar la configuración: " + ex.getMessage());
            return mostrar(model, configuracionRespaldoForm);
        }
        return "redirect:/admin/respaldos";
    }

    @GetMapping("/descargar/{nombre:.+}")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        Path archivo = backupService.archivoParaDescargar(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.getFileName().toString()).build().toString())
                .body(new FileSystemResource(archivo));
    }

    /** Nombre invalido o archivo borrado: volver a la lista con el mensaje. */
    @ExceptionHandler(IllegalArgumentException.class)
    public String archivoInvalido(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/admin/respaldos";
    }
}
