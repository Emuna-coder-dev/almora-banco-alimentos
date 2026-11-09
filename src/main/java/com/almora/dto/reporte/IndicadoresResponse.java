package com.almora.dto.reporte;

import java.util.Map;

/**
 * Tablero de indicadores (SCAMPER S1 y P1).
 * porcentajeEntregadoAntesDeVencer = entregadas / (entregadas + vencidas) * 100  -> indicador principal del proyecto.
 */
public record IndicadoresResponse(
        long donantesActivos,
        long fundacionesActivas,
        long totalDonaciones,
        Map<String, Long> donacionesPorEstado,
        long unidadesRecibidas,
        long unidadesEnBodega,
        long unidadesAsignadas,
        long unidadesEntregadas,
        long unidadesVencidas,
        double porcentajeEntregadoAntesDeVencer,
        long lotesEnRojo,
        long lotesEnAmarillo,
        long entregasRealizadas,
        long fundacionesAtendidas,
        long confirmacionesFallidas,
        /** Unidades recibidas por categoria de producto (grafico "Donaciones por tipo de producto"). */
        Map<String, Long> unidadesRecibidasPorCategoria,
        /** Unidades disponibles hoy por categoria. */
        Map<String, Long> unidadesEnBodegaPorCategoria
) {
}
