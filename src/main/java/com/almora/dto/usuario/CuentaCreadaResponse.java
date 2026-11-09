package com.almora.dto.usuario;

/**
 * Respuesta al crear una cuenta o restablecer su clave.
 * passwordTemporal solo aparece cuando el sistema genero la clave; se muestra UNA vez y no se guarda en claro.
 */
public record CuentaCreadaResponse(UsuarioResponse usuario, String passwordTemporal, String mensaje) {
}
