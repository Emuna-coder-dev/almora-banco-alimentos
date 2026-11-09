package com.almora.model.donacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Una donacion no se borra: se anula guardando motivo, fecha y responsable (HU07, SCAMPER R2). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Anulacion {
    private String motivo;
    private Instant fecha;
    private String responsable;
}
