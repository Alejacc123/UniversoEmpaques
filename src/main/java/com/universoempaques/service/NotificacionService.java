package com.universoempaques.service;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.Cotizacion;
import com.universoempaques.model.EstadoPedidoTipo;
import com.universoempaques.model.Pedido;
import com.universoempaques.repository.CotizacionRepository;
import com.universoempaques.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * RF-16: notificaciones automaticas por correo (patron Observer).
 *
 * Escucha dos eventos:
 *   - EstadoPedidoCambiadoEvent: avisa al CLIENTE cada vez que su pedido cambia de etapa.
 *   - CotizacionCambiadaEvent:   avisa a quien le toca responder la cotizacion
 *                                (cliente o comercial).
 *
 *  - @TransactionalEventListener: solo avisa si el cambio SI se guardo
 *    en la BD (despues del commit). Si algo falla, no se envia nada.
 *  - @Async: el correo se envia en segundo plano; el usuario que hizo
 *    el cambio no espera a que Gmail responda.
 *  - Si no hay correo configurado (spring.mail.username vacio en
 *    application-local.properties) no falla: deja el aviso en el log.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final String FIRMA = "\n\nUniverso Empaques";

    private final PedidoRepository pedidoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public NotificacionService(PedidoRepository pedidoRepository, CotizacionRepository cotizacionRepository,
                               ObjectProvider<JavaMailSender> mailSender) {
        this.pedidoRepository = pedidoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.mailSender = mailSender;
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarEstado(EstadoPedidoCambiadoEvent evento) {
        Pedido pedido = pedidoRepository.findById(evento.codigoPedido()).orElse(null);
        if (pedido == null || pedido.getCliente() == null) {
            return;
        }
        Cliente cliente = pedido.getCliente();
        String asunto = "Universo Empaques - Tu pedido PED-" + pedido.getCodigo() + " está: "
                + evento.nuevoEstado().getEtiqueta();
        enviar(cliente.getCorreo(), asunto, cuerpoPedido(cliente, pedido, evento));
    }

    private static String cuerpoPedido(Cliente cliente, Pedido pedido, EstadoPedidoCambiadoEvent evento) {
        StringBuilder texto = new StringBuilder()
                .append("Hola, ").append(cliente.getNombre()).append(".\n\n")
                .append(evento.nuevoEstado() == EstadoPedidoTipo.SOLICITADO
                        ? "Registramos tu pedido PED-" + pedido.getCodigo() + "."
                        : "Tu pedido PED-" + pedido.getCodigo() + " cambió a: " + evento.nuevoEstado().getEtiqueta() + ".")
                .append("\n");
        if (evento.reporte() != null && !evento.reporte().isBlank()) {
            texto.append("\nNota del equipo: ").append(evento.reporte()).append("\n");
        }
        return texto.append("\nPuedes ver el avance en la sección \"Mis pedidos\" de la plataforma.")
                .append(FIRMA).toString();
    }

    // ------------------------------------------------------------------
    // Cotizaciones
    // ------------------------------------------------------------------

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarCotizacion(CotizacionCambiadaEvent evento) {
        Cotizacion c = cotizacionRepository.findById(evento.codigoCotizacion()).orElse(null);
        if (c == null || c.getCliente() == null) {
            return;
        }
        String cot = "COT-" + c.getCodigo();
        String correoCliente = c.getCliente().getCorreo();
        String correoComercial = c.getUsuario() != null ? c.getUsuario().getCorreo() : null;

        switch (evento.nuevoEstado()) {
            case COTIZADA -> enviar(correoCliente, "Universo Empaques - Tu cotización " + cot + " ya tiene valor",
                    "Hola, " + c.getCliente().getNombre() + ".\n\nYa registramos el valor de tu cotización " + cot
                            + ": " + pesos(c.getValor()) + ".\n\nEntra a \"Mis cotizaciones\" para aprobarla, "
                            + "rechazarla o proponer otro valor." + FIRMA);
            case CONTRAOFERTA -> enviar(correoComercial, "Contraoferta en " + cot + " de " + c.getCliente().getNombre(),
                    c.getCliente().getNombre() + " propone " + pesos(c.getValorContraoferta())
                            + " para la cotización " + cot + " (valor cotizado: " + pesos(c.getValor()) + ")."
                            + (c.getObservaciones() != null ? "\nObservaciones: " + c.getObservaciones() : "")
                            + "\n\nRevísala en Comercial > Cotizaciones." + FIRMA);
            case APROBADA -> {
                if (evento.porComercial()) {
                    enviar(correoCliente, "Universo Empaques - Aceptamos tu contraoferta en " + cot,
                            "Hola, " + c.getCliente().getNombre() + ".\n\nAceptamos el valor que propusiste para "
                                    + cot + ": " + pesos(c.getValor()) + ". En breve generaremos tu pedido." + FIRMA);
                } else {
                    enviar(correoComercial, "Cotización " + cot + " aprobada por " + c.getCliente().getNombre(),
                            "El cliente aprobó la cotización " + cot + " por " + pesos(c.getValor()) + "."
                                    + (c.getObservaciones() != null ? "\nObservaciones: " + c.getObservaciones() : "")
                                    + "\n\nYa puedes generar el pedido en Comercial > Cotizaciones." + FIRMA);
                }
            }
            case RECHAZADA -> enviar(correoComercial, "Cotización " + cot + " rechazada por " + c.getCliente().getNombre(),
                    "El cliente rechazó la cotización " + cot + "."
                            + (c.getObservaciones() != null ? "\nMotivo: " + c.getObservaciones() : "") + FIRMA);
            default -> { /* SOLICITADA: Comercial la ve en su panel */ }
        }
    }

    private static String pesos(Double valor) {
        if (valor == null) return "—";
        NumberFormat f = NumberFormat.getIntegerInstance(new Locale("es", "CO"));
        return "$" + f.format(Math.round(valor));
    }

    // ------------------------------------------------------------------
    // Envio
    // ------------------------------------------------------------------

    /** ¿Hay correo configurado en application-local.properties? */
    public boolean correoConfigurado() {
        return mailSender.getIfAvailable() != null && remitente != null && !remitente.isBlank();
    }

    public String getRemitente() {
        return remitente;
    }

    /**
     * Envia un correo de prueba en el momento (sin @Async) para que el
     * administrador sepa si la configuracion funciona.
     * @return null si salio bien; si no, el motivo del error
     */
    public String enviarPrueba(String para) {
        if (!correoConfigurado()) {
            return "No hay correo configurado: agrega spring.mail.username y spring.mail.password en application-local.properties.";
        }
        try {
            mandar(para, "Universo Empaques - Correo de prueba",
                    "Si estás leyendo esto, las notificaciones por correo funcionan correctamente." + FIRMA);
            return null;
        } catch (Exception e) {
            return "El servidor de correo respondió con un error: " + e.getMessage();
        }
    }

    /** Envia o, si no hay correo configurado, deja el aviso en la consola. Nunca lanza errores. */
    private void enviar(String para, String asunto, String cuerpo) {
        if (para == null || para.isBlank()) {
            return;
        }
        if (!correoConfigurado()) {
            log.info("[Notificación simulada: correo no configurado] Para: {} | {}", para, asunto);
            return;
        }
        try {
            mandar(para, asunto, cuerpo);
            log.info("Notificación enviada a {}: {}", para, asunto);
        } catch (Exception e) {
            // Un correo que falla nunca debe afectar el pedido ni la cotizacion.
            log.warn("No se pudo enviar la notificación a {}: {}", para, e.getMessage());
        }
    }

    private void mandar(String para, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.getObject().send(mensaje);
    }
}
