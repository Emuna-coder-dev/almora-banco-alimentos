package com.almora.service;

import com.almora.config.AlmoraProperties;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.donacion.Confirmacion;
import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.ItemDonacion;
import com.almora.repository.DonacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Confirmacion por correo al donante (RF005 / HU06).
 *  - Se envia DESPUES de guardar la donacion y en segundo plano: el registro no espera al correo (RNF-R01).
 *  - Si el envio falla, la donacion NO se pierde: queda con confirmacion FALLIDA, se registra en auditoria
 *    y el administrador la ve en el listado (filtro confirmacion=FALLIDA) para reenviarla.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Bogota"));

    private final ObjectProvider<JavaMailSender> mailSender;
    private final DonacionRepository donacionRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoria;
    private final AlmoraProperties propiedades;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender, DonacionRepository donacionRepository,
                         MongoTemplate mongoTemplate, AuditoriaService auditoria, AlmoraProperties propiedades) {
        this.mailSender = mailSender;
        this.donacionRepository = donacionRepository;
        this.mongoTemplate = mongoTemplate;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRecibirDonacion(DonacionRecibidaEvent evento) {
        enviarConfirmacion(evento.donacionId());
    }

    /** Envia (o reenvia) la confirmacion y guarda el resultado en la donacion. */
    public Confirmacion enviarConfirmacion(String donacionId) {
        Donacion d = donacionRepository.findById(donacionId).orElse(null);
        if (d == null) {
            return null;
        }
        Confirmacion resultado;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (propiedades.mail() == null || !propiedades.mail().habilitado() || sender == null) {
            resultado = new Confirmacion(Confirmacion.Estado.DESHABILITADA, Instant.now(),
                    "El envio de correos esta deshabilitado (MAIL_ENABLED=false). La donacion quedo registrada.");
        } else {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(propiedades.mail().remitente());
                m.setTo(d.getDonanteCorreo());
                m.setSubject("ALMORA - Confirmacion de tu donacion " + d.getCodigo());
                m.setText(cuerpo(d));
                sender.send(m);
                resultado = new Confirmacion(Confirmacion.Estado.ENVIADA, Instant.now(), "Enviada a " + d.getDonanteCorreo());
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar la confirmacion de {}: {}", d.getCodigo(), e.getMessage());
                resultado = new Confirmacion(Confirmacion.Estado.FALLIDA, Instant.now(),
                        "No se pudo enviar el correo. Revisa la configuracion de correo y reenvia la confirmacion.");
                auditoria.registrarComo(null, null, "SISTEMA", AccionAuditoria.PROCESO_AUTOMATICO, "Donacion", d.getId(),
                        "Fallo el correo de confirmacion de " + d.getCodigo());
            }
        }
        // Se actualiza solo el campo "confirmacion" para no chocar con otros cambios de la donacion
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(d.getId())),
                Update.update("confirmacion", resultado), Donacion.class);
        return resultado;
    }

    private String cuerpo(Donacion d) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola ").append(d.getDonanteNombre()).append(",\n\n");
        sb.append("Gracias por tu donacion al banco de alimentos ALMORA. Quedo registrada con estos datos:\n\n");
        sb.append("Codigo de seguimiento: ").append(d.getCodigo()).append('\n');
        sb.append("Fecha de recepcion: ").append(d.getFechaRecepcion() == null ? "-" : FECHA.format(d.getFechaRecepcion())).append("\n\n");
        sb.append("Productos:\n");
        for (ItemDonacion i : d.getItems()) {
            sb.append(" - ").append(i.getNombreProducto()).append(": ").append(i.getCantidad()).append(' ')
                    .append(i.getUnidadMedida() == null ? "" : i.getUnidadMedida().name().toLowerCase());
            if (i.getFechaVencimiento() != null) {
                sb.append(" (vence ").append(i.getFechaVencimiento()).append(')');
            }
            sb.append('\n');
        }
        sb.append("\nTotal de unidades: ").append(d.getTotalUnidades()).append("\n\n");
        sb.append("Puedes seguir la ruta de tu donacion hasta la fundacion que la reciba y descargar tu certificado ")
                .append("desde tu cuenta, en la seccion \"Mis donaciones\".\n\n");
        sb.append("Equipo ALMORA");
        return sb.toString();
    }
}
