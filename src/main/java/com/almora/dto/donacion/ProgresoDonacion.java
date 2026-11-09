package com.almora.dto.donacion;

/** Donde estan hoy las unidades de una donacion. */
public record ProgresoDonacion(
        int total,
        int enBodega,
        int asignadas,
        int enRuta,
        int entregadas,
        int vencidas,
        int anuladas,
        int porcentajeEntregado
) {
}
