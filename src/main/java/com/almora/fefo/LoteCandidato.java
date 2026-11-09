package com.almora.fefo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lo minimo que el algoritmo FEFO necesita saber de un lote.
 * fechaVencimiento null = no vence (se usa de ultimo).
 */
public record LoteCandidato(String loteId, LocalDate fechaVencimiento, int cantidadDisponible, Instant fechaIngreso) {
}
