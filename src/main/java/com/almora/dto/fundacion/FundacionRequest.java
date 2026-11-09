package com.almora.dto.fundacion;

import com.almora.dto.comun.Validaciones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FundacionRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @Pattern(regexp = Validaciones.IDENTIFICACION, message = "NIT invalido") String nit,
        @NotBlank(message = "La persona de contacto es obligatoria") @Size(max = 120) String personaContacto,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.TELEFONO_MENSAJE) String telefono,
        @Email(message = "Correo invalido") @Size(max = 120) String correo,
        @NotBlank(message = "La direccion es obligatoria") @Size(max = 200) String direccion,
        @NotBlank(message = "La ciudad es obligatoria") @Size(max = 80) String ciudad
) {
}
