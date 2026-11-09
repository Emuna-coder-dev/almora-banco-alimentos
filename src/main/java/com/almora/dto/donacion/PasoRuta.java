package com.almora.dto.donacion;

import java.time.Instant;

/**
 * Un paso fijo de la "guia" de la donacion (como el rastreo de una empresa de mensajeria):
 * Registrada -> Recibida -> Almacenada -> Asignada -> En ruta -> Entregada.
 * completado = ya ocurrio; actual = es el punto donde esta la donacion ahora.
 */
public record PasoRuta(String estado, String titulo, String descripcion, boolean completado, boolean actual, Instant fecha) {
}
