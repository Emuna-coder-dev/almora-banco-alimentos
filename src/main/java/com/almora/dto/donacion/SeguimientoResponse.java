package com.almora.dto.donacion;

import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.donacion.EventoSeguimiento;

import java.util.List;

/**
 * Seguimiento de una donacion "tipo guia de mensajeria" (visible para los tres roles;
 * un donante solo ve las suyas).
 *  - ruta: los pasos fijos con cual ya se cumplio y cual es el actual (para dibujar la linea de tiempo).
 *  - eventos: el historial detallado, en orden cronologico.
 *  - destinos: a que fundaciones fue (o va) cada parte de la donacion.
 */
public record SeguimientoResponse(
        String donacionId,
        String codigo,
        String donanteNombre,
        EstadoDonacion estadoActual,
        String estadoTitulo,
        String estadoDescripcion,
        ProgresoDonacion progreso,
        List<PasoRuta> ruta,
        List<EventoSeguimiento> eventos,
        List<DestinoResponse> destinos
) {
}
