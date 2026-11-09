package com.almora.dto.entrega;

import com.almora.model.entrega.EstadoEntrega;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Avance de una entrega: PROGRAMADA -> EN_RUTA -> ENTREGADA, o CANCELADA.
 * receptor: quien recibio en la fundacion (al marcar ENTREGADA).
 * ubicacion: punto actual del recorrido (opcional, para el seguimiento).
 */
public record CambioEstadoEntregaRequest(
        @NotNull(message = "El estado es obligatorio") EstadoEntrega estado,
        @Size(max = 120) String receptor,
        @Size(max = 150) String ubicacion,
        @Size(max = 300) String observacion
) {
}
