package com.almora.dto.fundacion;

import com.almora.model.entrega.Fundacion;

import java.time.Instant;

public record FundacionResponse(String id, String nombre, String nit, String personaContacto, String telefono,
                                String correo, String direccion, String ciudad, boolean activa,
                                Instant fechaCreacion, Instant fechaActualizacion) {

    public static FundacionResponse de(Fundacion f) {
        return new FundacionResponse(f.getId(), f.getNombre(), f.getNit(), f.getPersonaContacto(), f.getTelefono(),
                f.getCorreo(), f.getDireccion(), f.getCiudad(), f.isActiva(), f.getFechaCreacion(), f.getFechaActualizacion());
    }
}
