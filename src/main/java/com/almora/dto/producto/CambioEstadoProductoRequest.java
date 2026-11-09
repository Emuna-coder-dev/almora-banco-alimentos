package com.almora.dto.producto;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoProductoRequest(@NotNull(message = "Indica si el producto queda activo") Boolean activo) {
}
