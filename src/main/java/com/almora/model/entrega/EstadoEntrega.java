package com.almora.model.entrega;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de una entrega a una fundacion.
 *   PROGRAMADA -> EN_RUTA -> ENTREGADA
 *   PROGRAMADA -> ENTREGADA            (la fundacion recoge en bodega)
 *   PROGRAMADA / EN_RUTA -> CANCELADA  (los productos regresan al inventario)
 */
public enum EstadoEntrega {
    PROGRAMADA("Entrega programada"),
    EN_RUTA("En ruta"),
    ENTREGADA("Entregada"),
    CANCELADA("Cancelada");

    private final String titulo;

    EstadoEntrega(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    public Set<EstadoEntrega> siguientesPermitidos() {
        return switch (this) {
            case PROGRAMADA -> EnumSet.of(EN_RUTA, ENTREGADA, CANCELADA);
            case EN_RUTA -> EnumSet.of(ENTREGADA, CANCELADA);
            case ENTREGADA, CANCELADA -> EnumSet.noneOf(EstadoEntrega.class);
        };
    }

    public boolean puedePasarA(EstadoEntrega destino) {
        return siguientesPermitidos().contains(destino);
    }

    public boolean estaActiva() {
        return this == PROGRAMADA || this == EN_RUTA;
    }
}
