package com.almora.dto.usuario;

import java.util.List;

/** "Mi impacto": lo que lograron las donaciones de un donante (SCAMPER P1). */
public record ImpactoDonanteResponse(
        long totalDonaciones,
        long unidadesDonadas,
        long unidadesEntregadas,
        long unidadesEnBodega,
        List<String> fundacionesAtendidas,
        long certificadosDisponibles
) {
}
