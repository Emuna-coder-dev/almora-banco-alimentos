package com.almora.dto.inventario;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.EstadoLote;
import com.almora.model.inventario.Semaforo;
import com.almora.model.inventario.UnidadMedida;

import java.time.Instant;
import java.time.LocalDate;

/** Un lote del inventario con su semaforo de vencimiento y su posicion en la cola FEFO. */
public record LoteResponse(
        String id,
        String codigo,
        String productoId,
        String nombreProducto,
        CategoriaProducto categoria,
        boolean perecedero,
        UnidadMedida unidadMedida,
        String donacionId,
        String codigoDonacion,
        int cantidadInicial,
        int cantidadDisponible,
        int cantidadAsignada,
        int cantidadEntregada,
        int cantidadVencida,
        int cantidadAnulada,
        LocalDate fechaVencimiento,
        Long diasParaVencer,
        Semaforo semaforo,
        EstadoLote estado,
        Instant fechaIngreso,
        String ubicacionAlmacen,
        /** 1 = el proximo en salir de ese producto segun FEFO. null si el lote no esta disponible. */
        Integer ordenFefo
) {
}
