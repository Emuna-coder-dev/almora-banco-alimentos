package com.almora.dto.entrega;

import java.util.List;

/**
 * Propuesta de asignacion FEFO ANTES de crear la entrega (SCAMPER R1:
 * "el sistema propone una asignacion; el administrador confirma o ajusta").
 * No reserva nada.
 */
public record PrevisualizacionEntregaResponse(String fundacionId, String fundacionNombre, boolean completa,
                                              List<PlanProductoResponse> productos) {
}
