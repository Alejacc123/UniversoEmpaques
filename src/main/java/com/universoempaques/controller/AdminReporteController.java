package com.universoempaques.controller;

import com.universoempaques.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** RF-18: reportes del administrador (pantalla y descarga en CSV para Excel). */
@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    private final ReporteService reporteService;

    public AdminReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                      Model model) {
        LocalDate[] rango = rango(desde, hasta);
        model.addAttribute("reporte", reporteService.generar(rango[0], rango[1]));
        return "interno/admin-reportes";
    }

    @GetMapping("/pedidos.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] rango = rango(desde, hasta);
        String csv = reporteService.pedidosCsv(reporteService.generar(rango[0], rango[1]));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("pedidos_" + rango[0] + "_a_" + rango[1] + ".csv").build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** Por defecto: desde el primer dia del mes hasta hoy. Si vienen al reves, se intercambian. */
    private static LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate d = desde != null ? desde : h.withDayOfMonth(1);
        return d.isAfter(h) ? new LocalDate[]{h, d} : new LocalDate[]{d, h};
    }
}
