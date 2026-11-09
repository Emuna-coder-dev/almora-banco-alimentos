package com.almora.service;

import com.almora.dto.comun.Pagina;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.auditoria.RegistroAuditoria;
import com.almora.repository.AuditoriaRepository;
import com.almora.security.SecurityUtils;
import com.almora.security.UsuarioAutenticado;
import com.almora.util.Textos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Bitacora de auditoria (RNF-AU01): quien hizo que, sobre que registro, cuando y desde que IP.
 * Registra creaciones, ediciones, cambios de estado, anulaciones, exportaciones e inicios de sesion.
 * Los registros solo se insertan; ningun endpoint permite editarlos ni borrarlos.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final AuditoriaRepository repositorio;
    private final MongoTemplate mongoTemplate;

    public AuditoriaService(AuditoriaRepository repositorio, MongoTemplate mongoTemplate) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
    }

    /** Registra una accion hecha por el usuario que esta autenticado en esta peticion. */
    public void registrar(AccionAuditoria accion, String entidad, String entidadId, String detalle) {
        UsuarioAutenticado actor = SecurityUtils.actualOpcional().orElse(null);
        if (actor == null) {
            registrarComo(null, null, "SISTEMA", accion, entidad, entidadId, detalle);
        } else {
            registrarComo(actor.id(), actor.correo(), actor.rol().name(), accion, entidad, entidadId, detalle);
        }
    }

    /** Registra una accion indicando el actor (se usa en login, cuando aun no hay usuario autenticado). */
    public void registrarComo(String usuarioId, String correo, String rol, AccionAuditoria accion,
                              String entidad, String entidadId, String detalle) {
        try {
            RegistroAuditoria r = new RegistroAuditoria();
            r.setFecha(Instant.now());
            r.setAccion(accion);
            r.setEntidad(entidad);
            r.setEntidadId(entidadId);
            r.setUsuarioId(usuarioId);
            r.setUsuarioCorreo(correo);
            r.setRol(rol);
            r.setDetalle(detalle == null ? null : detalle.length() > 500 ? detalle.substring(0, 500) : detalle);
            r.setIp(SecurityUtils.peticionActual().map(SecurityUtils::ipCliente).orElse(null));
            repositorio.save(r);
        } catch (RuntimeException e) {
            // La auditoria nunca debe tumbar la operacion principal, pero el fallo queda en el log.
            log.error("No se pudo guardar el registro de auditoria {} {} {}", accion, entidad, entidadId, e);
        }
    }

    public Pagina<RegistroAuditoria> buscar(AccionAuditoria accion, String entidad, String usuarioCorreo,
                                            String entidadId, LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        Criteria c = new Criteria();
        if (accion != null) {
            c.and("accion").is(accion);
        }
        if (!Textos.vacio(entidad)) {
            c.and("entidad").is(entidad.trim());
        }
        if (!Textos.vacio(entidadId)) {
            c.and("entidadId").is(entidadId.trim());
        }
        if (!Textos.vacio(usuarioCorreo)) {
            c.and("usuarioCorreo").is(Textos.normalizarCorreo(usuarioCorreo));
        }
        if (desde != null || hasta != null) {
            Criteria f = c.and("fecha");
            if (desde != null) {
                f.gte(desde.atStartOfDay(ZONA).toInstant());
            }
            if (hasta != null) {
                f.lt(hasta.plusDays(1).atStartOfDay(ZONA).toInstant());
            }
        }
        Query q = new Query(c);
        long total = mongoTemplate.count(q, RegistroAuditoria.class);
        q.with(Sort.by(Sort.Direction.DESC, "fecha")).skip((long) pagina * tamano).limit(tamano);
        List<RegistroAuditoria> lista = mongoTemplate.find(q, RegistroAuditoria.class);
        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return new Pagina<>(lista, pagina, tamano, total, totalPaginas);
    }
}
