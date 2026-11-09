package com.almora.dto.usuario;

import com.almora.dto.comun.Validaciones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El superadministrador crea un administrador (RF001 / HU01).
 * Si no envia contrasena, el sistema genera una temporal y obliga a cambiarla en el primer ingreso.
 */
public record CrearAdminRequest(
        @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") @Size(max = 120) String correo,
        @Pattern(regexp = Validaciones.CLAVE, message = Validaciones.CLAVE_MENSAJE) String password,
        @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.TELEFONO_MENSAJE) String telefono,
        @Size(max = 80) String cargo
) {
}
