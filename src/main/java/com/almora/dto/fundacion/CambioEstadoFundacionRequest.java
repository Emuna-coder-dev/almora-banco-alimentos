package com.almora.dto.fundacion;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoFundacionRequest(@NotNull(message = "Indica si la fundacion queda activa") Boolean activa) {
}
