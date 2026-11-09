package com.almora.dto.donacion;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.Semaforo;
import com.almora.model.inventario.UnidadMedida;

import java.time.LocalDate;

public record ItemDonacionResponse(
        String productoId,
        String nombreProducto,
        CategoriaProducto categoria,
        boolean perecedero,
        UnidadMedida unidadMedida,
        int cantidad,
        LocalDate fechaVencimiento,
        String codigoLote,
        Semaforo semaforo
) {
}
