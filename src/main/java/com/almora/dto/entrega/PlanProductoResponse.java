package com.almora.dto.entrega;

import java.util.List;

/** Resultado del algoritmo FEFO para un producto: de que lotes sale y cuanto falto. */
public record PlanProductoResponse(String productoId, String nombreProducto, int solicitado, int cubierto,
                                   int noCubierto, List<LoteAsignadoResponse> lotes) {
}
