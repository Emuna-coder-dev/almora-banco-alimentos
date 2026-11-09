package com.almora.dto.reporte;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Filtros del reporte (HU11): rango de fechas obligatorio y donante opcional. */
public record ReporteRequest(
        @NotNull(message = "La fecha inicial es obligatoria") LocalDate desde,
        @NotNull(message = "La fecha final es obligatoria") LocalDate hasta,
        String donanteId
) {
    @AssertTrue(message = "La fecha inicial no puede ser posterior a la final")
    public boolean isRangoValido() {
        return desde == null || hasta == null || !desde.isAfter(hasta);
    }
}
