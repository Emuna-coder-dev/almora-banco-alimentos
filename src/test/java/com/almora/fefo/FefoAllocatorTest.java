package com.almora.fefo;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del algoritmo FEFO: los 7 pasos del documento SCAMPER. */
class FefoAllocatorTest {

    private final FefoAllocator fefo = new FefoAllocator();
    private final LocalDate hoy = LocalDate.of(2026, 10, 14);
    private final Instant ingreso = Instant.parse("2026-10-01T10:00:00Z");

    private LoteCandidato lote(String id, LocalDate vence, int cantidad) {
        return new LoteCandidato(id, vence, cantidad, ingreso);
    }

    @Test
    void sacaPrimeroElLoteQueVenceAntes() {
        List<LoteCandidato> lotes = List.of(
                lote("lejano", hoy.plusDays(30), 10),
                lote("proximo", hoy.plusDays(2), 10),
                lote("medio", hoy.plusDays(10), 10));

        ResultadoFefo r = fefo.asignar(5, lotes, hoy);

        assertEquals(1, r.asignaciones().size());
        assertEquals("proximo", r.asignaciones().get(0).loteId());
        assertEquals(5, r.asignaciones().get(0).cantidad());
        assertTrue(r.completo());
    }

    @Test
    void recorreVariosLotesEnOrdenHastaCubrirLaCantidad() {
        List<LoteCandidato> lotes = List.of(
                lote("C", hoy.plusDays(20), 50),
                lote("A", hoy.plusDays(1), 4),
                lote("B", hoy.plusDays(5), 6));

        ResultadoFefo r = fefo.asignar(15, lotes, hoy);

        assertEquals(List.of(new AsignacionLote("A", 4), new AsignacionLote("B", 6), new AsignacionLote("C", 5)),
                r.asignaciones());
        assertEquals(15, r.cubierto());
        assertEquals(0, r.noCubierto());
    }

    @Test
    void informaLoQueNoSePudoCubrir() {
        ResultadoFefo r = fefo.asignar(30, List.of(lote("A", hoy.plusDays(3), 8), lote("B", hoy.plusDays(4), 7)), hoy);

        assertEquals(15, r.cubierto());
        assertEquals(15, r.noCubierto());
        assertTrue(!r.completo());
    }

    @Test
    void ignoraLotesVencidosYSinStock() {
        List<LoteCandidato> lotes = List.of(
                lote("vencido", hoy.minusDays(1), 100),
                lote("vacio", hoy.plusDays(1), 0),
                lote("bueno", hoy.plusDays(9), 5));

        ResultadoFefo r = fefo.asignar(3, lotes, hoy);

        assertEquals(List.of(new AsignacionLote("bueno", 3)), r.asignaciones());
    }

    @Test
    void elLoteQueVenceHoyTodaviaSePuedeEntregar() {
        ResultadoFefo r = fefo.asignar(2, List.of(lote("hoy", hoy, 2), lote("manana", hoy.plusDays(1), 2)), hoy);
        assertEquals("hoy", r.asignaciones().get(0).loteId());
    }

    @Test
    void losLotesSinFechaSalenDeUltimos() {
        List<LoteCandidato> lotes = List.of(lote("sinFecha", null, 10), lote("conFecha", hoy.plusDays(100), 3));

        ResultadoFefo r = fefo.asignar(5, lotes, hoy);

        assertEquals(List.of(new AsignacionLote("conFecha", 3), new AsignacionLote("sinFecha", 2)), r.asignaciones());
    }

    @Test
    void empateDeFechaSaleElQueIngresoPrimero() {
        LoteCandidato viejo = new LoteCandidato("viejo", hoy.plusDays(5), 5, Instant.parse("2026-09-01T00:00:00Z"));
        LoteCandidato nuevo = new LoteCandidato("nuevo", hoy.plusDays(5), 5, Instant.parse("2026-10-10T00:00:00Z"));

        ResultadoFefo r = fefo.asignar(5, List.of(nuevo, viejo), hoy);

        assertEquals("viejo", r.asignaciones().get(0).loteId());
    }

    @Test
    void sinLotesNoAsignaNada() {
        ResultadoFefo r = fefo.asignar(4, List.of(), hoy);
        assertTrue(r.asignaciones().isEmpty());
        assertEquals(4, r.noCubierto());
    }

    @Test
    void rechazaCantidadesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> fefo.asignar(0, List.of(), hoy));
        assertThrows(IllegalArgumentException.class, () -> fefo.asignar(-3, List.of(), hoy));
    }
}
