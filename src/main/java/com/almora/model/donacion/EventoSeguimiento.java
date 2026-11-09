package com.almora.model.donacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Un punto de la "ruta" de una donacion o una entrega, al estilo de una guia de mensajeria.
 * Ej.: "Recibida en bodega principal", "En ruta hacia Fundacion Niños Felices", "Entregada".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoSeguimiento {

    /** Estado al que corresponde el evento (texto libre para servir a donaciones y entregas). */
    private String estado;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    private Instant fecha;
    private String responsable;
    /** Codigo relacionado (por ejemplo, la entrega ENT-...). */
    private String referencia;

    public static EventoSeguimiento de(String estado, String titulo, String descripcion,
                                       String ubicacion, String responsable, String referencia) {
        return new EventoSeguimiento(estado, titulo, descripcion, ubicacion, Instant.now(), responsable, referencia);
    }
}
