package com.almora.dto.usuario;

import com.almora.dto.comun.Validaciones;
import com.almora.model.usuario.TipoDonante;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edicion de una cuenta por un administrador (RF002 / HU02, HU04).
 * Todos los campos son opcionales: solo se cambia lo que venga con valor.
 * El rol se cambia con su propio endpoint (solo superadministrador).
 */
public record ActualizarUsuarioRequest(
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres") String nombre,
        @Email(message = "Correo invalido") @Size(max = 120) String correo,
        @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.TELEFONO_MENSAJE) String telefono,
        @Size(max = 80) String cargo,
        TipoDonante tipoDonante,
        @Pattern(regexp = Validaciones.IDENTIFICACION, message = Validaciones.IDENTIFICACION_MENSAJE) String identificacion,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 120) String personaContacto
) {
}
