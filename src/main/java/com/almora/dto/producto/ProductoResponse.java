package com.almora.dto.producto;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.Producto;
import com.almora.model.inventario.UnidadMedida;

public record ProductoResponse(
        String id,
        String nombre,
        CategoriaProducto categoria,
        boolean perecedero,
        UnidadMedida unidadMedida,
        String descripcion,
        boolean activo
) {
    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.isPerecedero(),
                p.getUnidadMedida(), p.getDescripcion(), p.isActivo());
    }
}
