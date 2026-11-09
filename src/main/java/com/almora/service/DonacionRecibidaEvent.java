package com.almora.service;

/**
 * Se publica cuando una donacion entra al inventario. Cuando la transaccion termina bien,
 * CorreoService envia la confirmacion al donante en segundo plano (HU06).
 */
public record DonacionRecibidaEvent(String donacionId) {
}
