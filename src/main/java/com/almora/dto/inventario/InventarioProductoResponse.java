package com.almora.dto.inventario;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.Semaforo;
import com.almora.model.inventario.UnidadMedida;

import java.time.LocalDate;

/**
 * Existencias de un producto (HU09): cantidad, tipo y estado.
 * estadoStock: DISPONIBLE, POR_VENCER (hay lotes en rojo), AGOTADO.
 */
public record InventarioProductoResponse(
        String productoId,
        String nombre,
        CategoriaProducto categoria,
        boolean perecedero,
        UnidadMedida unidadMedida,
        long stockDisponible,
        long stockAsignado,
        int lotesDisponibles,
        LocalDate proximoVencimiento,
        Long diasParaVencer,
        Semaforo semaforo,
        String estadoStock
) {
}
