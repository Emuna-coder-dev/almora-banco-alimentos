package com.almora.dto.entrega;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Pedido de entrega a una fundacion. El sistema elige los lotes con FEFO (SCAMPER P2 y R1).
 * permitirParcial: si es false y no alcanza el inventario, no se crea la entrega.
 */
public record EntregaRequest(
        @NotBlank(message = "La fundacion es obligatoria") String fundacionId,
        @NotEmpty(message = "Indica al menos un producto")
        @Size(max = 50, message = "Maximo 50 productos por entrega") List<@Valid SolicitudItemRequest> items,
        @FutureOrPresent(message = "La fecha programada no puede estar en el pasado") LocalDate fechaProgramada,
        @Size(max = 500) String observaciones,
        Boolean permitirParcial
) {
}
