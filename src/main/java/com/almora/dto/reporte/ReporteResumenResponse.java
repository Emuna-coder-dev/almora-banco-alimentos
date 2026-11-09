package com.almora.dto.reporte;

import com.almora.model.auditoria.Reporte;

import java.time.Instant;
import java.time.LocalDate;

public record ReporteResumenResponse(String id, Instant fechaGeneracion, String generadoPor, LocalDate desde,
                                     LocalDate hasta, String donanteNombre, long totalDonaciones, long totalUnidades) {

    public static ReporteResumenResponse de(Reporte r) {
        return new ReporteResumenResponse(r.getId(), r.getFechaGeneracion(), r.getGeneradoPor(), r.getDesde(),
                r.getHasta(), r.getDonanteNombre(), r.getTotalDonaciones(), r.getTotalUnidades());
    }
}
