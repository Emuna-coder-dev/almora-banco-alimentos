package com.almora.fefo;

import com.almora.model.inventario.Semaforo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Semaforo SCAMPER A1: verde > 15 dias, amarillo 6 a 15, rojo 0 a 5 (HU10), vencido < 0. */
class CalculadoraSemaforoTest {

    private final CalculadoraSemaforo s = new CalculadoraSemaforo(15, 5);
    private final LocalDate hoy = LocalDate.of(2026, 10, 14);

    @Test
    void verdeCuandoFaltanMasDe15Dias() {
        assertEquals(Semaforo.VERDE, s.calcular(hoy.plusDays(16), hoy));
    }

    @Test
    void amarilloEntre6y15Dias() {
        assertEquals(Semaforo.AMARILLO, s.calcular(hoy.plusDays(15), hoy));
        assertEquals(Semaforo.AMARILLO, s.calcular(hoy.plusDays(6), hoy));
    }

    @Test
    void rojoCon5DiasOMenosIncluyendoHoy() {
        assertEquals(Semaforo.ROJO, s.calcular(hoy.plusDays(5), hoy));
        assertEquals(Semaforo.ROJO, s.calcular(hoy, hoy));
    }

    @Test
    void vencidoSiLaFechaYaPaso() {
        assertEquals(Semaforo.VENCIDO, s.calcular(hoy.minusDays(1), hoy));
    }

    @Test
    void sinFechaNoVence() {
        assertEquals(Semaforo.SIN_VENCIMIENTO, s.calcular(null, hoy));
    }

    @Test
    void umbralesInvalidosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new CalculadoraSemaforo(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new CalculadoraSemaforo(10, -1));
    }
}
