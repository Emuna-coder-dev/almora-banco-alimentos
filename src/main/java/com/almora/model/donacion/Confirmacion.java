package com.almora.model.donacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Resultado del correo de confirmacion al donante (HU06). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Confirmacion {

    public enum Estado { PENDIENTE, ENVIADA, FALLIDA, DESHABILITADA }

    private Estado estado;
    private Instant fecha;
    private String detalle;
}
