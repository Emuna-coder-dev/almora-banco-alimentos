package com.almora.dto.usuario;

import com.almora.dto.comun.Validaciones;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cada usuario edita sus propios datos. El correo y el rol no se cambian desde aqui. */
public record ActualizarPerfilRequest(
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres") String nombre,
        @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.TELEFONO_MENSAJE) String telefono,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 120) String personaContacto
) {
}
