package com.almora.dto.donacion;

import com.almora.model.donacion.Anulacion;
import com.almora.model.donacion.Confirmacion;
import com.almora.model.donacion.EstadoDonacion;

import java.time.Instant;
import java.util.List;

public record DonacionResponse(
        String id,
        String codigo,
        String donanteId,
        String donanteNombre,
        String donanteCorreo,
        List<ItemDonacionResponse> items,
        int totalUnidades,
        EstadoDonacion estado,
        String estadoTitulo,
        Instant fechaRegistro,
        Instant fechaRecepcion,
        Instant fechaAlmacenamiento,
        String ubicacionAlmacen,
        String registradaPor,
        String observaciones,
        Confirmacion confirmacion,
        boolean certificadoDisponible,
        String codigoCertificado,
        Anulacion anulacion
) {
}
