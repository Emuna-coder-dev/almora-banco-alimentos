package com.almora.exception;

import java.time.Instant;
import java.util.Map;

/** Formato unico de error que devuelve la API. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles
) {
    public static ErrorResponse de(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta, null);
    }
}
