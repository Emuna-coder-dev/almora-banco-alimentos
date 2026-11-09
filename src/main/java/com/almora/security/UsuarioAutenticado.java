package com.almora.security;

import com.almora.model.usuario.TipoRol;

/** Datos del usuario que hizo la peticion, extraidos del JWT y verificados contra la base de datos. */
public record UsuarioAutenticado(String id, String correo, String nombre, TipoRol rol) {

    public boolean esAdministrativo() {
        return rol.esAdministrativo();
    }

    public boolean esDonante() {
        return rol == TipoRol.DONANTE;
    }

    /** Texto para auditoria y seguimiento: "Ana Perez (ana@correo.com)". */
    public String etiqueta() {
        return nombre + " (" + correo + ")";
    }
}
