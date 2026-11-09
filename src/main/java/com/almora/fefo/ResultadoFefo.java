package com.almora.fefo;

import java.util.List;

/**
 * Resultado de asignar una cantidad de un producto.
 * noCubierto > 0 significa que el inventario no alcanzo (paso 7: se informa al administrador).
 */
public record ResultadoFefo(int solicitado, List<AsignacionLote> asignaciones, int noCubierto) {

    public int cubierto() {
        return solicitado - noCubierto;
    }

    public boolean completo() {
        return noCubierto == 0;
    }
}
