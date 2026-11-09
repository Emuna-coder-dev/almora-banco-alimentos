package com.almora.dto.entrega;

import com.almora.model.donacion.EventoSeguimiento;
import com.almora.model.entrega.Entrega;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.entrega.Faltante;
import com.almora.model.entrega.ItemEntrega;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EntregaResponse(
        String id,
        String codigo,
        String fundacionId,
        String fundacionNombre,
        String direccionDestino,
        String ciudadDestino,
        List<ItemEntrega> items,
        List<Faltante> faltantes,
        int totalUnidades,
        EstadoEntrega estado,
        String estadoTitulo,
        List<EventoSeguimiento> seguimiento,
        LocalDate fechaProgramada,
        Instant fechaCreacion,
        Instant fechaEntrega,
        String creadaPor,
        String observaciones,
        String receptor
) {
    public static EntregaResponse de(Entrega e) {
        return new EntregaResponse(e.getId(), e.getCodigo(), e.getFundacionId(), e.getFundacionNombre(),
                e.getDireccionDestino(), e.getCiudadDestino(), e.getItems(), e.getFaltantes(), e.totalUnidades(),
                e.getEstado(), e.getEstado().getTitulo(), e.getSeguimiento(), e.getFechaProgramada(),
                e.getFechaCreacion(), e.getFechaEntrega(), e.getCreadaPor(), e.getObservaciones(), e.getReceptor());
    }
}
