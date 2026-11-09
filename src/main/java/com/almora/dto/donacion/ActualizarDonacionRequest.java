package com.almora.dto.donacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Edicion de una donacion que aun esta PENDIENTE (todavia no entro al inventario). RF007. */
public record ActualizarDonacionRequest(
        @NotEmpty(message = "La donacion debe tener al menos un producto")
        @Size(max = 50, message = "Maximo 50 productos por donacion") List<@Valid ItemDonacionRequest> items,
        @Size(max = 500) String observaciones
) {
}
