package com.universoempaques.service;

import com.universoempaques.model.*;
import com.universoempaques.repository.DetallePedidoRepository;
import com.universoempaques.repository.DisenoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-12 (Diseno carga el diseno de cada producto del pedido) y
 * RF-13 (Comercial lo aprueba o pide ajustes, despues de hablar con el cliente).
 *
 * Reglas:
 *  - Solo se trabaja mientras el pedido esta "En diseno".
 *  - Cada carga es una VERSION nueva (1, 2, 3...). Se revisa la ultima.
 *  - El pedido pasa a produccion solo si la ultima version de TODOS sus
 *    productos esta APROBADA (lo revisa PedidoService.avanzarEstado).
 *  - Los archivos van en columnas MEDIUMBLOB; la app acepta hasta 5 MB por archivo.
 */
@Service
public class DisenoService {

    /** Maximo por archivo (5 MB, igual que spring.servlet.multipart.max-file-size). La columna MEDIUMBLOB admite 16 MB. */
    public static final int TAMANO_MAXIMO = 5 * 1024 * 1024;

    private final DisenoRepository disenoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoService pedidoService;

    public DisenoService(DisenoRepository disenoRepository, DetallePedidoRepository detallePedidoRepository,
                         PedidoService pedidoService) {
        this.disenoRepository = disenoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.pedidoService = pedidoService;
    }

    /** Un producto del pedido con todas las versiones de su diseno (la mas nueva primero). */
    public record DisenosDeProducto(DetallePedido detalle, List<Diseno> versiones) {
        public Diseno getUltimo() {
            return versiones.isEmpty() ? null : versiones.get(0);
        }
    }

    public List<DisenosDeProducto> listarPorPedido(Pedido pedido) {
        return pedidoService.listarDetalles(pedido).stream()
                .map(d -> new DisenosDeProducto(d, disenoRepository.findByDetallePedidoOrderByVersionDesc(d)))
                .toList();
    }

    /** ¿El pedido esta en la etapa de diseno? (solo ahi se sube y se revisa). */
    public boolean enEtapaDeDiseno(Pedido pedido) {
        EstadoPedido actual = pedidoService.estadoActual(pedido);
        return actual != null && actual.getEstado() == EstadoPedidoTipo.EN_DISENO;
    }

    /** RF-12: Diseno sube una nueva version del diseno de un producto. */
    @Transactional
    public Diseno subir(Integer codigoPedido, Integer codigoDetalle, MultipartFile archivo, MultipartFile logo,
                        String colorHex, String observaciones, Usuario disenador) throws IOException {
        DetallePedido detalle = detallePedidoRepository.findById(codigoDetalle)
                .filter(d -> d.getPedido().getCodigo().equals(codigoPedido))
                .orElseThrow(() -> new IllegalArgumentException("Ese producto no pertenece al pedido."));
        if (!enEtapaDeDiseno(detalle.getPedido())) {
            throw new IllegalArgumentException("Solo se pueden cargar diseños cuando el pedido está \"En diseño\".");
        }
        Diseno ultimo = disenoRepository.findTopByDetallePedidoOrderByVersionDesc(detalle).orElse(null);
        if (ultimo != null && ultimo.getEstado() == EstadoDisenoTipo.APROBADO) {
            throw new IllegalArgumentException("El diseño de este producto ya fue aprobado.");
        }
        if (ultimo != null && ultimo.getEstado() == EstadoDisenoTipo.PENDIENTE) {
            throw new IllegalArgumentException("La versión " + ultimo.getVersion()
                    + " todavía está pendiente de revisión; espera la respuesta antes de subir otra.");
        }

        Diseno diseno = new Diseno();
        diseno.setDetallePedido(detalle);
        diseno.setUsuario(disenador);
        diseno.setVersion(ultimo == null ? 1 : ultimo.getVersion() + 1);
        diseno.setEstado(EstadoDisenoTipo.PENDIENTE);
        diseno.setArchivoDiseno(leerArchivo(archivo, "El archivo del diseño", true));
        diseno.setLogo(leerArchivo(logo, "El logo", false));
        diseno.setColor(hexARgb(colorHex));
        diseno.setObservaciones(recortar(observaciones));
        return disenoRepository.save(diseno);
    }

    /**
     * RF-13: Comercial (o el admin) aprueba la ultima version o pide
     * ajustes. CodigoUsuario queda con quien la reviso (diccionario de datos).
     */
    @Transactional
    public Diseno revisar(Integer codigoPedido, Integer codigoDiseno, boolean aprobar, String comentario, Usuario revisor) {
        Diseno diseno = disenoRepository.findById(codigoDiseno)
                .filter(d -> d.getDetallePedido().getPedido().getCodigo().equals(codigoPedido))
                .orElseThrow(() -> new IllegalArgumentException("Ese diseño no pertenece al pedido."));
        if (!enEtapaDeDiseno(diseno.getDetallePedido().getPedido())) {
            throw new IllegalArgumentException("El pedido ya no está en la etapa de diseño.");
        }
        if (diseno.getEstado() != EstadoDisenoTipo.PENDIENTE) {
            throw new IllegalArgumentException("Esta versión ya fue revisada.");
        }
        String nota = recortar(comentario);
        if (!aprobar && nota == null) {
            throw new IllegalArgumentException("Escribe qué ajustes se necesitan, para que Diseño sepa qué cambiar.");
        }
        diseno.setEstado(aprobar ? EstadoDisenoTipo.APROBADO : EstadoDisenoTipo.AJUSTE_SOLICITADO);
        diseno.setUsuario(revisor);
        if (aprobar) {
            diseno.setFechaAprobado(LocalDate.now());
        }
        if (nota != null) {
            String previa = diseno.getObservaciones();
            diseno.setObservaciones(recortar((previa == null ? "" : previa + " | ") + "Revisión: " + nota));
        }
        return disenoRepository.save(diseno);
    }

    public Diseno buscar(Integer codigo) {
        return disenoRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El diseño no existe."));
    }

    // ------------------------------------------------------------------

    /** Lee el archivo y valida tamano (maximo 5 MB) y tipo (PNG, JPG o PDF). */
    private static byte[] leerArchivo(MultipartFile archivo, String nombre, boolean obligatorio) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            if (obligatorio) throw new IllegalArgumentException(nombre + " es obligatorio.");
            return null;
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException(nombre + " pesa " + String.format("%.1f", archivo.getSize() / (1024.0 * 1024))
                    + " MB; el máximo es 5 MB. Redúcelo o expórtalo en menor calidad.");
        }
        byte[] bytes = archivo.getBytes();
        if (tipoDeContenido(bytes) == null) {
            throw new IllegalArgumentException(nombre + " debe ser una imagen PNG, JPG o un PDF.");
        }
        return bytes;
    }

    /** Reconoce el tipo por sus primeros bytes ("firma"), no por el nombre, que se puede falsear. */
    public static String tipoDeContenido(byte[] b) {
        if (b == null || b.length < 4) return null;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "image/jpeg";
        if (b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "application/pdf";
        return null;
    }

    /** "#4b93b6" -> "rgb(75,147,182)" (el modelo guarda el color en formato RGB). */
    static String hexARgb(String hex) {
        if (hex == null || !hex.matches("^#[0-9a-fA-F]{6}$")) return null;
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "rgb(" + r + "," + g + "," + b + ")";
    }

    private static String recortar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }
}
