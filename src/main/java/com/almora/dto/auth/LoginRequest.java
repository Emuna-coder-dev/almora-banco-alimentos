package com.almora.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") @Size(max = 120) String correo,
        @NotBlank(message = "La contrasena es obligatoria") @Size(max = 72) String password
) {
}
