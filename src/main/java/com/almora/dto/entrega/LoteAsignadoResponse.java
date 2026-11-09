package com.almora.dto.entrega;

import com.almora.model.inventario.Semaforo;

import java.time.LocalDate;

public record LoteAsignadoResponse(String loteId, String codigoLote, String codigoDonacion, int cantidad,
                                   LocalDate fechaVencimiento, Long diasParaVencer, Semaforo semaforo) {
}
