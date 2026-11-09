package com.almora.model.entrega;

import com.almora.model.donacion.EventoSeguimiento;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrega de productos del inventario a una fundacion (SCAMPER P2).
 * Sus items salen de los lotes elegidos por FEFO. Su avance alimenta el seguimiento de cada donacion.
 */
@Getter
@Setter
@Document(collection = "entregas")
@CompoundIndexes({
        @CompoundIndex(name = "items_donacion_idx", def = "{'items.donacionId': 1}"),
        @CompoundIndex(name = "fundacion_fecha_idx", def = "{'fundacionId': 1, 'fechaCreacion': -1}")
})
public class Entrega {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String fundacionId;
    private String fundacionNombre;
    private String direccionDestino;
    private String ciudadDestino;

    private List<ItemEntrega> items = new ArrayList<>();
    private List<Faltante> faltantes = new ArrayList<>();

    @Indexed
    private EstadoEntrega estado;

    private List<EventoSeguimiento> seguimiento = new ArrayList<>();

    private LocalDate fechaProgramada;
    private Instant fechaCreacion;
    private Instant fechaEntrega;
    private String creadaPor;
    private String observaciones;
    /** Nombre de quien recibe en la fundacion. */
    private String receptor;

    @Version
    private Long version;

    public int totalUnidades() {
        return items.stream().mapToInt(ItemEntrega::getCantidad).sum();
    }
}
