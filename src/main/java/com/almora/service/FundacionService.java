package com.almora.service;

import com.almora.dto.comun.Pagina;
import com.almora.dto.entrega.EntregaResponse;
import com.almora.dto.fundacion.FundacionRequest;
import com.almora.dto.fundacion.FundacionResponse;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.entrega.Fundacion;
import com.almora.repository.EntregaRepository;
import com.almora.repository.FundacionRepository;
import com.almora.util.Textos;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/** Fundaciones receptoras: el destino final de las donaciones (SCAMPER P2). */
@Service
public class FundacionService {

    private final FundacionRepository repositorio;
    private final EntregaRepository entregaRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoria;

    public FundacionService(FundacionRepository repositorio, EntregaRepository entregaRepository,
                            MongoTemplate mongoTemplate, AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.entregaRepository = entregaRepository;
        this.mongoTemplate = mongoTemplate;
        this.auditoria = auditoria;
    }

    public FundacionResponse crear(FundacionRequest req) {
        String nit = Textos.limpiar(req.nit());
        if (nit != null && repositorio.existsByNit(nit)) {
            throw ApiException.conflicto("Ya existe una fundacion con ese NIT");
        }
        Fundacion f = new Fundacion();
        copiar(req, f, nit);
        f.setActiva(true);
        Fundacion guardada = repositorio.save(f);
        auditoria.registrar(AccionAuditoria.CREAR, "Fundacion", guardada.getId(), guardada.getNombre());
        return FundacionResponse.de(guardada);
    }

    public FundacionResponse actualizar(String id, FundacionRequest req) {
        Fundacion f = buscar(id);
        String nit = Textos.limpiar(req.nit());
        if (nit != null) {
            repositorio.findByNit(nit).ifPresent(otra -> {
                if (!otra.getId().equals(id)) {
                    throw ApiException.conflicto("Ya existe otra fundacion con ese NIT");
                }
            });
        }
        copiar(req, f, nit);
        Fundacion guardada = repositorio.save(f);
        auditoria.registrar(AccionAuditoria.ACTUALIZAR, "Fundacion", id, f.getNombre());
        return FundacionResponse.de(guardada);
    }

    public FundacionResponse cambiarEstado(String id, boolean activa) {
        Fundacion f = buscar(id);
        f.setActiva(activa);
        Fundacion guardada = repositorio.save(f);
        auditoria.registrar(activa ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, "Fundacion", id, f.getNombre());
        return FundacionResponse.de(guardada);
    }

    public List<FundacionResponse> listar(String texto, Boolean activa) {
        Criteria c = new Criteria();
        if (activa != null) {
            c.and("activa").is(activa);
        }
        if (!Textos.vacio(texto)) {
            c.and("nombre").regex(Textos.escaparRegex(texto), "i");
        }
        return mongoTemplate.find(new Query(c).with(Sort.by("nombre")).limit(500), Fundacion.class)
                .stream().map(FundacionResponse::de).toList();
    }

    public FundacionResponse obtener(String id) {
        return FundacionResponse.de(buscar(id));
    }

    /** Historial de entregas de una fundacion, de la mas reciente a la mas antigua (SCAMPER P2). */
    public Pagina<EntregaResponse> historial(String id, int pagina, int tamano) {
        buscar(id);
        return Pagina.de(entregaRepository.findByFundacionIdOrderByFechaCreacionDesc(id, PageRequest.of(pagina, tamano)),
                EntregaResponse::de);
    }

    public Fundacion buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Fundacion no encontrada"));
    }

    private void copiar(FundacionRequest req, Fundacion f, String nit) {
        f.setNombre(Textos.limpiar(req.nombre()));
        f.setNit(nit);
        f.setPersonaContacto(Textos.limpiar(req.personaContacto()));
        f.setTelefono(Textos.limpiar(req.telefono()));
        f.setCorreo(Textos.normalizarCorreo(Textos.limpiar(req.correo())));
        f.setDireccion(Textos.limpiar(req.direccion()));
        f.setCiudad(Textos.limpiar(req.ciudad()));
    }
}
