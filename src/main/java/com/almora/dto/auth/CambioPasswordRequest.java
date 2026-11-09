package com.almora.dto.auth;

import com.almora.dto.comun.Validaciones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria") @Size(max = 72) String actual,
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Pattern(regexp = Validaciones.CLAVE, message = Validaciones.CLAVE_MENSAJE) String nueva
) {
}
