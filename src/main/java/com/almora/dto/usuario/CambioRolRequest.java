package com.almora.dto.usuario;

import com.almora.model.usuario.TipoRol;
import jakarta.validation.constraints.NotNull;

public record CambioRolRequest(@NotNull(message = "El rol es obligatorio") TipoRol rol) {
}
