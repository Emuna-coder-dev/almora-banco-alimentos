package com.almora.model.usuario;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Superadministrador: hereda todo lo que hace un Admin y ademas
 * crea y administra a los administradores (HU01).
 */
@TypeAlias("superadmin")
@Document(collection = "usuarios")
public class SuperAdmin extends Admin {

    public SuperAdmin() {
        super(TipoRol.SUPERADMIN);
    }

    /** Gestiona donantes y administradores, pero no a otros superadministradores. */
    @Override
    public boolean puedeGestionar(Rol otro) {
        return otro instanceof Donante || (otro instanceof Admin && !(otro instanceof SuperAdmin));
    }

    @Override
    public boolean puedeGestionarAdministradores() {
        return true;
    }
}
