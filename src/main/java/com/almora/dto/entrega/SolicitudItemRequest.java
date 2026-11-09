package com.almora.dto.entrega;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** "La fundacion necesita X unidades de este producto" (paso 1 del algoritmo FEFO). */
public record SolicitudItemRequest(
        @NotBlank(message = "El producto es obligatorio") String productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        @Max(value = 1_000_000, message = "La cantidad es demasiado grande") Integer cantidad
) {
}
