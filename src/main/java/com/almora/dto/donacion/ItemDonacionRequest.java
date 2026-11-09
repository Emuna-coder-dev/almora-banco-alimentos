package com.almora.dto.donacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Un producto dentro de la donacion. La fecha de vencimiento es obligatoria si el producto es perecedero (HU05). */
public record ItemDonacionRequest(
        @NotBlank(message = "El producto es obligatorio") String productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        @Max(value = 1_000_000, message = "La cantidad es demasiado grande") Integer cantidad,
        LocalDate fechaVencimiento
) {
}
