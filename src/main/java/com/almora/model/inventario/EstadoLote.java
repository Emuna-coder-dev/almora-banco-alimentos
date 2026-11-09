package com.almora.model.inventario;

public enum EstadoLote {
    /** Tiene unidades disponibles para entregar. */
    DISPONIBLE,
    /** Todas sus unidades ya fueron asignadas a entregas. */
    AGOTADO,
    /** Llego su fecha de vencimiento con unidades sin entregar (HU10). */
    VENCIDO,
    /** La donacion de origen fue anulada (SCAMPER R2). */
    ANULADO
}
