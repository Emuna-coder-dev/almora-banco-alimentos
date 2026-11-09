package com.almora.model.donacion;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de una donacion (HU08, SCAMPER A2). Es lo que ve el usuario en el "seguimiento".
 *
 *  PENDIENTE -> RECIBIDA -> ALMACENADA -> ASIGNADA -> EN_RUTA -> (PARCIALMENTE_)ENTREGADA
 *       \           \            \
 *        +-----------+------------+--> ANULADA         (y VENCIDA si caduca sin entregarse)
 *
 * Equivalencia con HU08: recibida = RECIBIDA, almacenada = ALMACENADA, distribuida = ENTREGADA, vencida = VENCIDA.
 *
 * Solo PENDIENTE->RECIBIDA y RECIBIDA->ALMACENADA las cambia un administrador a mano.
 * ASIGNADA, EN_RUTA, PARCIALMENTE_ENTREGADA, ENTREGADA y VENCIDA se calculan solas a partir de las entregas y lotes.
 * ANULADA se hace con el endpoint de anulacion (con motivo).
 */
public enum EstadoDonacion {
    PENDIENTE("Pendiente de recibir", "El donante registro la donacion y el banco aun no la recibe"),
    RECIBIDA("Recibida", "El banco de alimentos recibio los productos"),
    ALMACENADA("Almacenada", "Los productos fueron clasificados por lote y guardados en bodega"),
    ASIGNADA("Asignada", "Los productos fueron asignados a una entrega para una fundacion"),
    EN_RUTA("En ruta", "Los productos van en camino hacia la fundacion"),
    PARCIALMENTE_ENTREGADA("Parcialmente entregada", "Una parte ya llego a su destino y el resto sigue en bodega"),
    ENTREGADA("Entregada", "Los productos llegaron a su destino"),
    VENCIDA("Vencida", "Los productos vencieron antes de poder entregarse"),
    ANULADA("Anulada", "La donacion fue anulada");

    private final String titulo;
    private final String descripcion;

    EstadoDonacion(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Transiciones que un administrador puede pedir manualmente. */
    public Set<EstadoDonacion> transicionesManuales() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(RECIBIDA);
            case RECIBIDA -> EnumSet.of(ALMACENADA);
            default -> EnumSet.noneOf(EstadoDonacion.class);
        };
    }

    public boolean puedeAnularse() {
        return this == PENDIENTE || this == RECIBIDA || this == ALMACENADA;
    }

    /** Estados en los que ya existe soporte de la donacion (se puede emitir certificado). */
    public boolean permiteCertificado() {
        return this != PENDIENTE && this != ANULADA;
    }
}
