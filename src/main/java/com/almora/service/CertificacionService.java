package com.almora.service;

import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.donacion.Donacion;
import com.almora.model.usuario.Rol;
import com.almora.repository.DonacionRepository;
import com.almora.repository.UsuarioRepository;
import com.almora.util.Codigos;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emision y verificacion de certificados de donacion (SCAMPER C1).
 * El donante descarga el suyo desde "Mis donaciones"; los administradores pueden descargar cualquiera.
 */
@Service
public class CertificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Bogota"));

    private final DonacionService donacionService;
    private final DonacionRepository donacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CertificadoService pdf;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoria;

    public CertificacionService(DonacionService donacionService, DonacionRepository donacionRepository,
                                UsuarioRepository usuarioRepository, CertificadoService pdf,
                                MongoTemplate mongoTemplate, AuditoriaService auditoria) {
        this.donacionService = donacionService;
        this.donacionRepository = donacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.pdf = pdf;
        this.mongoTemplate = mongoTemplate;
        this.auditoria = auditoria;
    }

    /** Genera el PDF. La primera vez asigna el codigo de verificacion (despues siempre es el mismo). */
    public byte[] certificado(String donacionId, String urlBase) {
        Donacion d = donacionService.buscarConPermiso(donacionId);
        if (!d.getEstado().permiteCertificado()) {
            throw ApiException.conflicto("El certificado esta disponible cuando la donacion ya fue recibida "
                    + "por el banco de alimentos (y no esta anulada)");
        }
        if (d.getCodigoCertificado() == null) {
            String codigo = Codigos.generar("CERT", 8);
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(d.getId()).and("codigoCertificado").exists(false)),
                    Update.update("codigoCertificado", codigo), Donacion.class);
            d = donacionService.buscar(donacionId);
        }
        Rol donante = usuarioRepository.findById(d.getDonanteId()).orElse(null);
        String url = urlBase + "/api/publico/certificados/" + d.getCodigoCertificado();
        byte[] archivo = pdf.generar(d, donante, url);
        auditoria.registrar(AccionAuditoria.EXPORTAR, "Donacion", d.getId(), "Certificado " + d.getCodigoCertificado());
        return archivo;
    }

    /** Verificacion publica: confirma que el certificado existe, sin exponer datos de contacto. */
    public Map<String, Object> verificar(String codigoCertificado) {
        Donacion d = donacionRepository.findByCodigoCertificado(codigoCertificado == null ? "" : codigoCertificado.trim().toUpperCase())
                .orElseThrow(() -> ApiException.noEncontrado("No existe un certificado con ese codigo"));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("valido", d.getEstado().permiteCertificado());
        r.put("codigoCertificado", d.getCodigoCertificado());
        r.put("codigoDonacion", d.getCodigo());
        r.put("donante", d.getDonanteNombre());
        r.put("fecha", FECHA.format(d.getFechaRecepcion() != null ? d.getFechaRecepcion() : d.getFechaRegistro()));
        r.put("totalUnidades", d.getTotalUnidades());
        r.put("productos", d.getItems().size());
        r.put("estado", d.getEstado().getTitulo());
        return r;
    }
}
