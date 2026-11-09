package com.almora.model.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Administrador del banco de alimentos.
 * Gestiona donantes, productos, donaciones, inventario, entregas y reportes.
 * NO puede crear ni administrar otros administradores.
 */
@Getter
@Setter
@TypeAlias("admin")
@Document(collection = "usuarios")
public class Admin extends Rol {

    private String cargo;

    public Admin() {
        super(TipoRol.ADMIN);
    }

    /** Usado por SuperAdmin para heredar todo lo del administrador con otro rol. */
    protected Admin(TipoRol rol) {
        super(rol);
    }

    /** Un administrador solo gestiona donantes. */
    @Override
    public boolean puedeGestionar(Rol otro) {
        return otro instanceof Donante;
    }
}
