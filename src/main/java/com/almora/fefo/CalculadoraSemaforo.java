package com.almora.fefo;

import com.almora.model.inventario.Semaforo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Semaforo de vencimiento (SCAMPER A1, HU10). Java puro para poder probarlo.
 *   VERDE:    faltan mas de diasAmarillo dias
 *   AMARILLO: faltan entre diasRojo+1 y diasAmarillo dias
 *   ROJO:     faltan diasRojo dias o menos (incluye hoy) -> es la "alerta" de HU10
 *   VENCIDO:  la fecha ya paso
 */
public final class CalculadoraSemaforo {

    private final int diasAmarillo;
    private final int diasRojo;

    public CalculadoraSemaforo(int diasAmarillo, int diasRojo) {
        if (diasRojo < 0 || diasAmarillo <= diasRojo) {
            throw new IllegalArgumentException("Debe cumplirse 0 <= diasRojo < diasAmarillo");
        }
        this.diasAmarillo = diasAmarillo;
        this.diasRojo = diasRojo;
    }

    public Semaforo calcular(LocalDate fechaVencimiento, LocalDate hoy) {
        if (fechaVencimiento == null) {
            return Semaforo.SIN_VENCIMIENTO;
        }
        long dias = diasParaVencer(fechaVencimiento, hoy);
        if (dias < 0) {
            return Semaforo.VENCIDO;
        }
        if (dias <= diasRojo) {
            return Semaforo.ROJO;
        }
        if (dias <= diasAmarillo) {
            return Semaforo.AMARILLO;
        }
        return Semaforo.VERDE;
    }

    public static long diasParaVencer(LocalDate fechaVencimiento, LocalDate hoy) {
        return ChronoUnit.DAYS.between(hoy, fechaVencimiento);
    }

    public int getDiasAmarillo() {
        return diasAmarillo;
    }

    public int getDiasRojo() {
        return diasRojo;
    }
}
