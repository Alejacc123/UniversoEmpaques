package com.universoempaques.service;

import com.universoempaques.model.Cliente;
import com.universoempaques.model.EstadoPedidoTipo;
import com.universoempaques.model.Pedido;
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

/**
 * RF-16: notificacion automatica por correo al cliente cada vez que
 * cambia el estado de su pedido (observador del EstadoPedidoCambiadoEvent).
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

    private final PedidoRepository pedidoRepository;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public NotificacionService(PedidoRepository pedidoRepository, ObjectProvider<JavaMailSender> mailSender) {
        this.pedidoRepository = pedidoRepository;
        this.mailSender = mailSender;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarEstado(EstadoPedidoCambiadoEvent evento) {
        Pedido pedido = pedidoRepository.findById(evento.codigoPedido()).orElse(null);
        if (pedido == null || pedido.getCliente() == null || pedido.getCliente().getCorreo() == null) {
            return;
        }
        Cliente cliente = pedido.getCliente();
        String asunto = "Universo Empaques - Tu pedido PED-" + pedido.getCodigo() + " está: "
                + evento.nuevoEstado().getEtiqueta();
        String cuerpo = armarCuerpo(cliente, pedido, evento);

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || remitente == null || remitente.isBlank()) {
            log.info("[Notificación simulada: correo no configurado] Para: {} | {}", cliente.getCorreo(), asunto);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(cliente.getCorreo());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            sender.send(mensaje);
            log.info("Notificación enviada a {}: {}", cliente.getCorreo(), asunto);
        } catch (Exception e) {
            // Un correo que falla nunca debe afectar el pedido.
            log.warn("No se pudo enviar la notificación a {}: {}", cliente.getCorreo(), e.getMessage());
        }
    }

    private static String armarCuerpo(Cliente cliente, Pedido pedido, EstadoPedidoCambiadoEvent evento) {
        StringBuilder texto = new StringBuilder()
                .append("Hola, ").append(cliente.getNombre()).append(".\n\n")
                .append(evento.nuevoEstado() == EstadoPedidoTipo.SOLICITADO
                        ? "Registramos tu pedido PED-" + pedido.getCodigo() + "."
                        : "Tu pedido PED-" + pedido.getCodigo() + " cambió a: " + evento.nuevoEstado().getEtiqueta() + ".")
                .append("\n");
        if (evento.reporte() != null && !evento.reporte().isBlank()) {
            texto.append("\nNota del equipo: ").append(evento.reporte()).append("\n");
        }
        texto.append("\nPuedes ver el avance en la sección \"Mis pedidos\" de la plataforma.\n\n")
                .append("Universo Empaques");
        return texto.toString();
    }
}
