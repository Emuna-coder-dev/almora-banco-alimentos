package com.almora.dto.usuario;

import com.almora.dto.comun.Validaciones;
import com.almora.model.usuario.TipoDonante;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Un administrador registra un donante (RF003 / HU03).
 * Obligatorios: nombre o razon social, tipo, contacto (correo y telefono) y direccion.
 */
public record CrearDonanteRequest(
        @NotBlank(message = "El nombre o razon social es obligatorio") @Size(max = 120) String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") @Size(max = 120) String correo,
        @Pattern(regexp = Validaciones.CLAVE, message = Validaciones.CLAVE_MENSAJE) String password,
        @NotBlank(message = "El telefono de contacto es obligatorio")
        @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.TELEFONO_MENSAJE) String telefono,
        @NotNull(message = "El tipo de donante es obligatorio") TipoDonante tipoDonante,
        @Pattern(regexp = Validaciones.IDENTIFICACION, message = Validaciones.IDENTIFICACION_MENSAJE) String identificacion,
        @NotBlank(message = "La direccion es obligatoria") @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 120) String personaContacto
) {
}
