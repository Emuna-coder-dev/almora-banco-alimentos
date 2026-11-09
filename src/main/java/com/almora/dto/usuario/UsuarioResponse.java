package com.almora.dto.usuario;

import com.almora.model.usuario.Admin;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.Rol;
import com.almora.model.usuario.TipoDonante;
import com.almora.model.usuario.TipoRol;

import java.time.Instant;

/** Datos publicos de una cuenta. NUNCA incluye la contrasena ni su hash (SCAMPER M2). */
public record UsuarioResponse(
        String id,
        String nombre,
        String correo,
        TipoRol rol,
        String telefono,
        boolean activo,
        boolean debeCambiarPassword,
        Instant ultimoAcceso,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        // Solo donantes
        TipoDonante tipoDonante,
        String identificacion,
        String direccion,
        String ciudad,
        String personaContacto,
        // Solo administradores
        String cargo
) {
    public static UsuarioResponse de(Rol u) {
        TipoDonante tipo = null;
        String identificacion = null, direccion = null, ciudad = null, contacto = null, cargo = null;
        if (u instanceof Donante d) {
            tipo = d.getTipoDonante();
            identificacion = d.getIdentificacion();
            direccion = d.getDireccion();
            ciudad = d.getCiudad();
            contacto = d.getPersonaContacto();
        }
        if (u instanceof Admin a) {
            cargo = a.getCargo();
        }
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getTelefono(),
                u.isActivo(), u.isDebeCambiarPassword(), u.getUltimoAcceso(), u.getFechaCreacion(),
                u.getFechaActualizacion(), tipo, identificacion, direccion, ciudad, contacto, cargo);
    }
}
