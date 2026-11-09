package com.almora.dto.producto;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.UnidadMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio") @Size(max = 100) String nombre,
        @NotNull(message = "La categoria es obligatoria") CategoriaProducto categoria,
        @NotNull(message = "Indica si el producto es perecedero") Boolean perecedero,
        @NotNull(message = "La unidad de medida es obligatoria") UnidadMedida unidadMedida,
        @Size(max = 300) String descripcion
) {
}
