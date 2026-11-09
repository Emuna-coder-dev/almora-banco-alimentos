package com.almora.model.auditoria;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Bitacora de quien hizo que y cuando (RNF-AU01). Solo se inserta, nunca se edita. */
@Getter
@Setter
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;

    @Indexed
    private Instant fecha;

    @Indexed
    private AccionAuditoria accion;

    @Indexed
    private String entidad;
    private String entidadId;

    private String usuarioId;
    @Indexed
    private String usuarioCorreo;
    private String rol;

    private String detalle;
    private String ip;
}
