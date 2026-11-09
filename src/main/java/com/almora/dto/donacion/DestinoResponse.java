package com.almora.dto.donacion;

import com.almora.model.entrega.EstadoEntrega;

import java.time.Instant;
import java.time.LocalDate;

/** Hacia donde fue (o va) una parte de la donacion: fundacion, ciudad, unidades y estado de la entrega. */
public record DestinoResponse(
        String codigoEntrega,
        String fundacion,
        String direccion,
        String ciudad,
        EstadoEntrega estado,
        String estadoTitulo,
        int unidades,
        LocalDate fechaProgramada,
        Instant fechaEntrega,
        String receptor
) {
}
