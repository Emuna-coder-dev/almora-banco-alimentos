package com.almora.controller;

/**
 * Reglas de acceso por rol usadas en @PreAuthorize (RNF-S01).
 * Como SuperAdmin hereda de Admin, todo lo que puede ADMIN tambien lo puede SUPERADMIN.
 */
public final class Permisos {

    private Permisos() {
    }

    public static final String SUPERADMIN = "hasRole('SUPERADMIN')";
    public static final String ADMINISTRATIVO = "hasAnyRole('ADMIN','SUPERADMIN')";
    public static final String DONANTE = "hasRole('DONANTE')";
    public static final String CUALQUIERA = "hasAnyRole('ADMIN','SUPERADMIN','DONANTE')";
}
