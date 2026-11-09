package com.almora.dto.inventario;

import java.util.List;

/**
 * Alerta de productos proximos a vencer (HU10).
 * Lotes que vencen dentro de los proximos "diasAlerta" dias, ordenados del mas proximo al mas lejano.
 */
public record AlertaVencimientoResponse(int diasAlerta, int totalLotes, long totalUnidadesEnRiesgo, List<LoteResponse> lotes) {
}
