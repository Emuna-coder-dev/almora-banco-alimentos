package com.almora.dto.donacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Anular una donacion exige un motivo (RF006 / HU07, SCAMPER R2). */
public record AnulacionRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres") String motivo
) {
}
