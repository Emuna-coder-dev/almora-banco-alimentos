package com.almora.dto.inventario;

/** Cuantos lotes con stock hay en cada color del semaforo (SCAMPER A1). */
public record ResumenSemaforoResponse(long verde, long amarillo, long rojo, long vencido, long sinVencimiento,
                                      int diasAmarillo, int diasRojo) {
}
