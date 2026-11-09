package com.almora.dto.donacion;

import com.almora.model.donacion.EstadoDonacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambio manual de estado (RF007 / HU08). Solo se permiten PENDIENTE->RECIBIDA y RECIBIDA->ALMACENADA;
 * los demas estados los calcula el sistema a partir de las entregas.
 */
public record CambioEstadoDonacionRequest(
        @NotNull(message = "El estado es obligatorio") EstadoDonacion estado,
        @Size(max = 120) String ubicacionAlmacen,
        @Size(max = 300) String observacion
) {
}
