package com.almora.model.usuario;

/**
 * Los tres roles del sistema.
 * SUPERADMIN: todo, incluido crear y administrar administradores.
 * ADMIN: todo, excepto administrar administradores (solo administra donantes).
 * DONANTE: sus donaciones, su seguimiento y sus certificados.
 */
public enum TipoRol {
    SUPERADMIN,
    ADMIN,
    DONANTE;

    /** Nombre de la autoridad que usa Spring Security ("ROLE_ADMIN", etc.). */
    public String autoridad() {
        return "ROLE_" + name();
    }

    public boolean esAdministrativo() {
        return this == SUPERADMIN || this == ADMIN;
    }
}
