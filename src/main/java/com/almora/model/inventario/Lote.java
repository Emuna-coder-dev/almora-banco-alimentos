package com.almora.model.inventario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lote: una cantidad concreta de UN producto, con SU fecha de vencimiento, que llego en UNA donacion.
 * Es la unidad con la que trabaja el algoritmo FEFO.
 *
 * Cuentas del lote (siempre se cumple):
 *   cantidadInicial = cantidadDisponible + cantidadAsignada + cantidadEntregada + cantidadVencida + cantidadAnulada
 *
 * Las cantidades se modifican SOLO con operaciones atomicas ($inc) y condicionales,
 * para que dos entregas simultaneas nunca dejen el stock en negativo (SCAMPER M2).
 */
@Getter
@Setter
@Document(collection = "lotes")
@CompoundIndex(name = "fefo_idx", def = "{'productoId': 1, 'estado': 1, 'fechaVencimiento': 1}")
public class Lote {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String productoId;
    private String nombreProducto;
    private CategoriaProducto categoria;
    private boolean perecedero;
    private UnidadMedida unidadMedida;

    @Indexed
    private String donacionId;
    private String codigoDonacion;
    private String donanteId;

    private int cantidadInicial;
    private int cantidadDisponible;
    /** Reservada en entregas programadas o en ruta. */
    private int cantidadAsignada;
    private int cantidadEntregada;
    private int cantidadVencida;
    /** Unidades retiradas porque la donacion de origen se anulo. */
    private int cantidadAnulada;

    /** null = el producto no vence. */
    private LocalDate fechaVencimiento;
    private Instant fechaIngreso;
    private String ubicacionAlmacen;

    @Indexed
    private EstadoLote estado;
}
