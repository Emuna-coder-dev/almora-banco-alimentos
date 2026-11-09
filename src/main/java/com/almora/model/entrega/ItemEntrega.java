package com.almora.model.entrega;

import com.almora.model.inventario.UnidadMedida;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Cantidad tomada de un lote para una entrega (resultado del algoritmo FEFO). */
@Getter
@Setter
public class ItemEntrega {
    private String loteId;
    private String codigoLote;
    private String productoId;
    private String nombreProducto;
    private UnidadMedida unidadMedida;
    private int cantidad;
    private LocalDate fechaVencimiento;
    private String donacionId;
    private String codigoDonacion;
}
