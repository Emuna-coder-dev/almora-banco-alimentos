package com.almora.model.donacion;

import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.UnidadMedida;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Un renglon de la donacion: producto + cantidad + vencimiento (SCAMPER M2: varios productos por donacion).
 * Se guarda una copia del nombre y la categoria para que el historial no cambie si el catalogo cambia.
 */
@Getter
@Setter
public class ItemDonacion {
    private String productoId;
    private String nombreProducto;
    private CategoriaProducto categoria;
    private boolean perecedero;
    private UnidadMedida unidadMedida;
    private int cantidad;
    private LocalDate fechaVencimiento;
    /** Se llenan cuando la donacion se recibe y se crea el lote. */
    private String loteId;
    private String codigoLote;
}
