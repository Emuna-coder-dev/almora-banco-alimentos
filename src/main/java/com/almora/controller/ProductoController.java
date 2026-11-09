package com.almora.controller;

import com.almora.dto.producto.CambioEstadoProductoRequest;
import com.almora.dto.producto.ProductoRequest;
import com.almora.dto.producto.ProductoResponse;
import com.almora.model.inventario.CategoriaProducto;
import com.almora.security.SecurityUtils;
import com.almora.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalogo de productos. Todos lo consultan (el donante lo necesita para donar); solo administradores lo modifican. */
@Tag(name = "4. Productos")
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Listar el catalogo (los donantes solo ven productos activos)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) CategoriaProducto categoria,
                                         @RequestParam(required = false) Boolean perecedero,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        boolean soloActivos = SecurityUtils.actual().esDonante() || !incluirInactivos;
        return productoService.listar(categoria, perecedero, q, soloActivos);
    }

    @Operation(summary = "Ver un producto")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable String id) {
        return productoService.obtener(id);
    }

    @Operation(summary = "Crear producto")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest req) {
        return productoService.crear(req);
    }

    @Operation(summary = "Editar producto")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable String id, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(id, req);
    }

    @Operation(summary = "Activar o desactivar producto (no se borran para conservar el historial)")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoProductoRequest req) {
        return productoService.cambiarEstado(id, req.activo());
    }
}
