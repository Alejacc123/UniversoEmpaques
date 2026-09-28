package com.universoempaques.service;

import com.universoempaques.dto.RegistrarCotizacionForm;
import com.universoempaques.dto.SolicitarCotizacionForm;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoCotizacionTipo;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.CotizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;

    public CotizacionService(CotizacionRepository cotizacionRepository, ClienteRepository clienteRepository) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteRepository = clienteRepository;
    }

    // ------------------------------------------------------------------
    // Area comercial
    // ------------------------------------------------------------------

    /**
     * Lista las cotizaciones filtradas por estado. Si el estado es null
     * devuelve todas, de la mas reciente a la mas antigua.
     */
    public List<Cotizacion> listar(EstadoCotizacionTipo estado) {
        if (estado == null) {
            return cotizacionRepository.findAllByOrderByFechaSolicitudDesc();
        }
        return cotizacionRepository.findByEstadoOrderByFechaSolicitudAsc(estado);
    }

    public long contarPorEstado(EstadoCotizacionTipo estado) {
        return cotizacionRepository.countByEstado(estado);
    }

    public Cotizacion buscarPorId(Integer codigo) {
        return cotizacionRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La cotizacion no existe."));
    }

    /**
     * RF-07: el area comercial registra el valor y las condiciones de una
     * cotizacion SOLICITADA. La entidad valida la transicion de estado y
     * lanza IllegalStateException si la cotizacion ya fue registrada.
     */
    @Transactional
    public Cotizacion registrar(Integer codigo, RegistrarCotizacionForm form, Usuario comercial) {
        Cotizacion cotizacion = buscarPorId(codigo);
        cotizacion.registrar(form.getValorEstimado(), form.getCondiciones().trim(), comercial);
        return cotizacionRepository.save(cotizacion);
    }

    // ------------------------------------------------------------------
    // Cliente
    // ------------------------------------------------------------------

    /**
     * RF-06: el cliente solicita una cotizacion. Queda en estado SOLICITADA
     * con la fecha actual (ver @PrePersist en Cotizacion).
     */
    @Transactional
    public Cotizacion solicitar(SolicitarCotizacionForm form, Cliente clienteSesion) {
        Cliente cliente = clienteRepository.findById(clienteSesion.getNit())
                .orElseThrow(() -> new IllegalArgumentException("Tu cuenta de cliente no existe."));

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setMaterial(form.getMaterial().trim());
        cotizacion.setMedidas(componerMedidas(form.getLargo(), form.getAncho(), form.getAlto()));
        cotizacion.setCantidad(form.getCantidad());
        cotizacion.setDescripcion(textoOpcional(form.getDescripcion()));
        return cotizacionRepository.save(cotizacion);
    }

    public List<Cotizacion> listarDeCliente(Cliente cliente) {
        return cotizacionRepository.findByClienteOrderByFechaSolicitudDesc(cliente);
    }

    /** Devuelve la cotizacion solo si pertenece al cliente que la consulta. */
    public Optional<Cotizacion> buscarDeCliente(Integer codigo, Cliente cliente) {
        return cotizacionRepository.findByCodigoAndCliente(codigo, cliente);
    }

    public long contarDeClientePorEstado(Cliente cliente, EstadoCotizacionTipo estado) {
        return cotizacionRepository.countByClienteAndEstado(cliente, estado);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Une las medidas en un texto legible, ej: "30 x 20 x 15.5 cm". */
    private String componerMedidas(BigDecimal largo, BigDecimal ancho, BigDecimal alto) {
        StringBuilder medidas = new StringBuilder()
                .append(formatear(largo)).append(" x ").append(formatear(ancho));
        if (alto != null) {
            medidas.append(" x ").append(formatear(alto));
        }
        return medidas.append(" cm").toString();
    }

    private String formatear(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private String textoOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
