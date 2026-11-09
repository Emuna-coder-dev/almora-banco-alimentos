package com.almora.dto.usuario;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoUsuarioRequest(@NotNull(message = "Indica si la cuenta queda activa") Boolean activo) {
}
