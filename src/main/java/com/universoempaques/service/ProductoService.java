package com.universoempaques.service;

import com.universoempaques.dto.ProductoForm;
import com.universoempaques.model.Producto;
import com.universoempaques.repository.DetallePedidoRepository;
import com.universoempaques.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de productos (Administracion > Productos).
 *
 * Cambiar el precio aqui NO cambia los pedidos ya registrados: cada linea
 * de pedido guarda su propio PrecioUnitario (el precio del momento).
 * Un producto que ya se uso en algun pedido no se puede borrar (se perderia
 * el historial); se puede editar.
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ProductoService(ProductoRepository productoRepository, DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    public List<Producto> listar() {
        return productoRepository.findAllByOrderByNombreAsc();
    }

    /** Cuantas veces se ha pedido cada producto (para la tabla y para saber si se puede borrar). */
    public Map<Integer, Long> usosPorProducto(List<Producto> productos) {
        Map<Integer, Long> usos = new LinkedHashMap<>();
        for (Producto p : productos) {
            usos.put(p.getCodigo(), detallePedidoRepository.countByProducto(p));
        }
        return usos;
    }

    public Producto buscar(Integer codigo) {
        return productoRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
    }

    public ProductoForm formDe(Producto p) {
        ProductoForm form = new ProductoForm();
        form.setCodigo(p.getCodigo());
        form.setNombre(p.getNombre());
        form.setMaterial(p.getMaterial());
        form.setPrecio(p.getPrecio() == null ? null : p.getPrecio().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        form.setForma(p.getForma());
        form.setTamano(p.getTamano());
        return form;
    }

    @Transactional
    public Producto guardar(ProductoForm form) {
        String nombre = form.getNombre().trim();
        boolean repetido = form.getCodigo() == null
                ? productoRepository.existsByNombreIgnoreCase(nombre)
                : productoRepository.existsByNombreIgnoreCaseAndCodigoNot(nombre, form.getCodigo());
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }

        Producto producto = form.getCodigo() == null ? new Producto() : buscar(form.getCodigo());
        producto.setNombre(nombre);
        producto.setMaterial(form.getMaterial().trim());
        producto.setPrecio(new BigDecimal(form.getPrecio()));
        producto.setForma(vacioANull(form.getForma()));
        producto.setTamano(normalizarTamano(form.getTamano()));
        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminar(Integer codigo) {
        Producto producto = buscar(codigo);
        long usos = detallePedidoRepository.countByProducto(producto);
        if (usos > 0) {
            throw new IllegalArgumentException("\"" + producto.getNombre() + "\" ya está en " + usos
                    + " línea(s) de pedido y no se puede eliminar. Puedes editarlo (por ejemplo, cambiar su precio).");
        }
        productoRepository.delete(producto);
    }

    /** "30 X 20 x15cm" -> "30x20x15 cm", para que todos se vean igual. */
    static String normalizarTamano(String tamano) {
        if (tamano == null || tamano.isBlank()) return null;
        String t = tamano.trim().replaceAll("\\s*[xX]\\s*", "x");
        return t.replaceAll("\\s*(cm|mm)$", " $1");
    }

    private static String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
