package com.almora.dto.auth;

import com.almora.dto.usuario.UsuarioResponse;

/**
 * Respuesta del login y del refresh.
 * El token de acceso viaja aqui; el de refresco viaja SOLO en una cookie HttpOnly (no la puede leer JavaScript).
 */
public record AuthResponse(String accessToken, String tipo, long expiraEnSegundos, UsuarioResponse usuario) {

    public static AuthResponse bearer(String token, long segundos, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", segundos, usuario);
    }
}
