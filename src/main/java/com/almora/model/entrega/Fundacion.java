package com.almora.model.entrega;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Fundacion receptora: el destino final de las donaciones (SCAMPER P2). */
@Getter
@Setter
@Document(collection = "fundaciones")
public class Fundacion {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true, sparse = true)
    private String nit;

    private String personaContacto;
    private String telefono;
    private String correo;
    private String direccion;
    private String ciudad;
    private boolean activa = true;

    @CreatedDate
    private Instant fechaCreacion;

    @LastModifiedDate
    private Instant fechaActualizacion;
}
