package com.almora.service;

import com.almora.dto.producto.ProductoRequest;
import com.almora.dto.producto.ProductoResponse;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.Producto;
import com.almora.repository.ProductoRepository;
import com.almora.util.Textos;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Catalogo de productos (SCAMPER S2). El producto solo describe QUE es;
 * las cantidades y fechas de vencimiento viven en los lotes.
 * Un producto con historial no se borra: se desactiva.
 */
@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoria;

    public ProductoService(ProductoRepository repositorio, MongoTemplate mongoTemplate, AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
        this.auditoria = auditoria;
    }

    public ProductoResponse crear(ProductoRequest req) {
        String nombre = Textos.limpiar(req.nombre());
        String normalizado = Producto.normalizar(nombre);
        if (repositorio.findByNombreNormalizado(normalizado).isPresent()) {
            throw ApiException.conflicto("El producto '" + nombre + "' ya existe en el catalogo");
        }
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setNombreNormalizado(normalizado);
        p.setCategoria(req.categoria());
        p.setPerecedero(req.perecedero());
        p.setUnidadMedida(req.unidadMedida());
        p.setDescripcion(Textos.limpiar(req.descripcion()));
        p.setActivo(true);
        Producto guardado = repositorio.save(p);
        auditoria.registrar(AccionAuditoria.CREAR, "Producto", guardado.getId(), nombre);
        return ProductoResponse.de(guardado);
    }

    public ProductoResponse actualizar(String id, ProductoRequest req) {
        Producto p = buscar(id);
        String nombre = Textos.limpiar(req.nombre());
        String normalizado = Producto.normalizar(nombre);
        repositorio.findByNombreNormalizado(normalizado).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw ApiException.conflicto("Ya existe otro producto llamado '" + nombre + "'");
            }
        });
        p.setNombre(nombre);
        p.setNombreNormalizado(normalizado);
        p.setCategoria(req.categoria());
        p.setPerecedero(req.perecedero());
        p.setUnidadMedida(req.unidadMedida());
        p.setDescripcion(Textos.limpiar(req.descripcion()));
        Producto guardado = repositorio.save(p);
        auditoria.registrar(AccionAuditoria.ACTUALIZAR, "Producto", id, nombre);
        return ProductoResponse.de(guardado);
    }

    public ProductoResponse cambiarEstado(String id, boolean activo) {
        Producto p = buscar(id);
        p.setActivo(activo);
        Producto guardado = repositorio.save(p);
        auditoria.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, "Producto", id, p.getNombre());
        return ProductoResponse.de(guardado);
    }

    /** Catalogo con filtros. Los donantes solo ven productos activos. */
    public List<ProductoResponse> listar(CategoriaProducto categoria, Boolean perecedero, String texto, boolean soloActivos) {
        Criteria c = new Criteria();
        if (soloActivos) {
            c.and("activo").is(true);
        }
        if (categoria != null) {
            c.and("categoria").is(categoria);
        }
        if (perecedero != null) {
            c.and("perecedero").is(perecedero);
        }
        if (!Textos.vacio(texto)) {
            c.and("nombre").regex(Textos.escaparRegex(texto), "i");
        }
        Query q = new Query(c).with(Sort.by("nombre")).limit(500);
        return mongoTemplate.find(q, Producto.class).stream().map(ProductoResponse::de).toList();
    }

    public ProductoResponse obtener(String id) {
        return ProductoResponse.de(buscar(id));
    }

    public Producto buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Producto no encontrado"));
    }
}
