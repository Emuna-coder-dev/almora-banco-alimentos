package com.almora.model.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persona o empresa que dona alimentos (HU03, HU04).
 * Puede ver sus donaciones, su seguimiento y descargar sus certificados.
 */
@Getter
@Setter
@TypeAlias("donante")
@Document(collection = "usuarios")
public class Donante extends Rol {

    private TipoDonante tipoDonante;

    /** Cedula o NIT. Unico, pero opcional (sparse). Permite buscar al donante (HU04). */
    @Indexed(unique = true, sparse = true)
    private String identificacion;

    private String direccion;
    private String ciudad;

    /** Para empresas: persona de contacto. */
    private String personaContacto;

    public Donante() {
        super(TipoRol.DONANTE);
    }

    /** Un donante no administra a nadie. */
    @Override
    public boolean puedeGestionar(Rol otro) {
        return false;
    }
}
