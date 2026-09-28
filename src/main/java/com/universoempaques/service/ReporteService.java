package com.universoempaques.service;

import com.universoempaques.model.*;
import com.universoempaques.repository.CotizacionRepository;
import com.universoempaques.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-18: reportes del administrador sobre pedidos, cotizaciones y
 * tiempos de produccion y entrega, para un rango de fechas.
 * Todo se calcula con los datos que ya existen (no hay tablas nuevas).
 */
@Service
public class ReporteService {

    private final PedidoRepository pedidoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PedidoService pedidoService;

    public ReporteService(PedidoRepository pedidoRepository, CotizacionRepository cotizacionRepository,
                          PedidoService pedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.pedidoService = pedidoService;
    }

    /** Una fila de conteo para las barras: etiqueta, cantidad y % respecto al mayor. */
    public record Conteo(String etiqueta, long cantidad, int porcentaje) { }

    /** Tiempo promedio que un pedido pasa en una etapa. */
    public record TiempoEtapa(String etapa, String promedio, long muestras) { }

    /** Producto mas pedido en el periodo. */
    public record ProductoVendido(String nombre, long unidades, BigDecimal valor) { }

    /** Fila del listado de pedidos (tambien se usa para el CSV). */
    public record FilaPedido(Pedido pedido, EstadoPedidoTipo estado, BigDecimal total) { }

    /** Todo el reporte de un periodo. */
    public record Reporte(LocalDate desde, LocalDate hasta,
                          int totalPedidos, long entregados, BigDecimal valorPedidos,
                          List<Conteo> pedidosPorEstado,
                          int totalCotizaciones, List<Conteo> cotizacionesPorEstado,
                          Integer tasaAprobacion, BigDecimal valorPromedioCotizado,
                          List<TiempoEtapa> tiempos, String tiempoTotalEntrega,
                          List<ProductoVendido> topProductos, List<FilaPedido> pedidos) { }

    public Reporte generar(LocalDate desde, LocalDate hasta) {
        List<Pedido> pedidos = pedidoRepository.findByFechaRegistroBetweenOrderByCodigoAsc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay().minusNanos(1));

        // ---- Pedidos: estado actual y valor ----
        List<FilaPedido> filas = new ArrayList<>();
        Map<EstadoPedidoTipo, Long> porEstado = new EnumMap<>(EstadoPedidoTipo.class);
        Map<String, long[]> unidadesPorProducto = new LinkedHashMap<>();
        Map<String, BigDecimal> valorPorProducto = new HashMap<>();
        Map<EstadoPedidoTipo, List<Duration>> duraciones = new EnumMap<>(EstadoPedidoTipo.class);
        List<Duration> totalesEntrega = new ArrayList<>();
        BigDecimal valorPedidos = BigDecimal.ZERO;

        for (Pedido p : pedidos) {
            List<DetallePedido> detalles = pedidoService.listarDetalles(p);
            BigDecimal total = pedidoService.calcularTotal(detalles);
            valorPedidos = valorPedidos.add(total);
            EstadoPedido actual = pedidoService.estadoActual(p);
            EstadoPedidoTipo estado = actual != null ? actual.getEstado() : EstadoPedidoTipo.SOLICITADO;
            porEstado.merge(estado, 1L, Long::sum);
            filas.add(new FilaPedido(p, estado, total));

            for (DetallePedido d : detalles) {
                String nombre = d.getProducto().getNombre();
                unidadesPorProducto.computeIfAbsent(nombre, k -> new long[1])[0] += d.getCantidad() == null ? 0 : d.getCantidad();
                valorPorProducto.merge(nombre, d.getSubtotal(), BigDecimal::add);
            }

            // ---- Tiempos: cuanto duro cada etapa ya cerrada ----
            for (EstadoPedido e : pedidoService.historialEstados(p)) {
                if (e.getFechaInicio() != null && e.getFechaFin() != null && !e.getEstado().esFinal()) {
                    duraciones.computeIfAbsent(e.getEstado(), k -> new ArrayList<>())
                            .add(Duration.between(e.getFechaInicio(), e.getFechaFin()));
                }
                if (e.getEstado().esFinal() && e.getFechaInicio() != null && p.getFechaRegistro() != null) {
                    totalesEntrega.add(Duration.between(p.getFechaRegistro(), e.getFechaInicio()));
                }
            }
        }

        List<Conteo> pedidosPorEstado = conteos(Arrays.stream(EstadoPedidoTipo.values())
                .collect(Collectors.toMap(EstadoPedidoTipo::getEtiqueta, e -> porEstado.getOrDefault(e, 0L),
                        (a, b) -> a, LinkedHashMap::new)));

        List<TiempoEtapa> tiempos = Arrays.stream(EstadoPedidoTipo.values())
                .filter(e -> !e.esFinal())
                .map(e -> {
                    List<Duration> lista = duraciones.getOrDefault(e, List.of());
                    return new TiempoEtapa(e.getEtiqueta(), lista.isEmpty() ? "—" : legible(promedio(lista)), lista.size());
                })
                .toList();

        List<ProductoVendido> top = unidadesPorProducto.entrySet().stream()
                .map(en -> new ProductoVendido(en.getKey(), en.getValue()[0], valorPorProducto.getOrDefault(en.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingLong(ProductoVendido::unidades).reversed())
                .limit(5)
                .toList();

        // ---- Cotizaciones ----
        List<Cotizacion> cotizaciones = cotizacionRepository.findByFechaSolicitudBetween(desde, hasta);
        Map<EstadoCotizacionTipo, Long> cotPorEstado = cotizaciones.stream()
                .collect(Collectors.groupingBy(Cotizacion::getEstado, () -> new EnumMap<>(EstadoCotizacionTipo.class), Collectors.counting()));
        List<Conteo> cotizacionesPorEstado = conteos(Arrays.stream(EstadoCotizacionTipo.values())
                .collect(Collectors.toMap(EstadoCotizacionTipo::getEtiqueta, e -> cotPorEstado.getOrDefault(e, 0L),
                        (a, b) -> a, LinkedHashMap::new)));
        long aprobadas = cotPorEstado.getOrDefault(EstadoCotizacionTipo.APROBADA, 0L);
        long rechazadas = cotPorEstado.getOrDefault(EstadoCotizacionTipo.RECHAZADA, 0L);
        Integer tasa = (aprobadas + rechazadas) == 0 ? null : (int) Math.round(aprobadas * 100.0 / (aprobadas + rechazadas));
        OptionalDouble promedioValor = cotizaciones.stream().filter(c -> c.getValor() != null).mapToDouble(Cotizacion::getValor).average();

        return new Reporte(desde, hasta, pedidos.size(), porEstado.getOrDefault(EstadoPedidoTipo.ENTREGADO, 0L),
                valorPedidos, pedidosPorEstado, cotizaciones.size(), cotizacionesPorEstado, tasa,
                promedioValor.isPresent() ? BigDecimal.valueOf(promedioValor.getAsDouble()) : null,
                tiempos, totalesEntrega.isEmpty() ? "—" : legible(promedio(totalesEntrega)), top, filas);
    }

    /** CSV (separado por ";" para que Excel en espanol lo abra en columnas). */
    public String pedidosCsv(Reporte r) {
        StringBuilder csv = new StringBuilder("﻿"); // BOM: Excel reconoce las tildes
        csv.append("Pedido;Cliente;NIT;Registrado;Entrega estimada;Estado;Total\n");
        for (FilaPedido f : r.pedidos()) {
            Pedido p = f.pedido();
            csv.append("PED-").append(p.getCodigo()).append(';')
                    .append(celda(p.getCliente() != null ? p.getCliente().getNombre() : "")).append(';')
                    .append(celda(p.getCliente() != null ? p.getCliente().getNit() : "")).append(';')
                    .append(p.getFechaRegistro() != null ? p.getFechaRegistro().toLocalDate() : "").append(';')
                    .append(p.getFechaEntrega() != null ? p.getFechaEntrega().toLocalDate() : "").append(';')
                    .append(f.estado().getEtiqueta()).append(';')
                    .append(f.total().toPlainString()).append('\n');
        }
        return csv.toString();
    }

    // ------------------------------------------------------------------

    private static List<Conteo> conteos(Map<String, Long> valores) {
        long maximo = valores.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return valores.entrySet().stream()
                .map(e -> new Conteo(e.getKey(), e.getValue(), maximo == 0 ? 0 : (int) Math.round(e.getValue() * 100.0 / maximo)))
                .toList();
    }

    private static Duration promedio(List<Duration> lista) {
        return Duration.ofSeconds((long) lista.stream().mapToLong(Duration::getSeconds).average().orElse(0));
    }

    /** 3700 s -> "1 h 1 min"; 2 dias y 4 horas -> "2 d 4 h". */
    static String legible(Duration d) {
        long dias = d.toDays(), horas = d.toHoursPart(), minutos = d.toMinutesPart();
        if (dias > 0) return dias + " d " + horas + " h";
        if (horas > 0) return horas + " h " + minutos + " min";
        return Math.max(minutos, d.getSeconds() > 0 ? 1 : 0) + " min";
    }

    /** Evita que un ";" o salto de linea en un nombre rompa las columnas del CSV. */
    private static String celda(String texto) {
        String t = texto.replace("\"", "\"\"");
        return (t.contains(";") || t.contains("\n") || t.contains("\"")) ? "\"" + t + "\"" : t;
    }
}
