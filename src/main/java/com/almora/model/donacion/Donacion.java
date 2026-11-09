package com.almora.model.donacion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Donacion en especie. Contiene varios productos (items) y su ruta de seguimiento.
 * Al recibirse, cada item se convierte en un Lote del inventario.
 */
@Getter
@Setter
@Document(collection = "donaciones")
@CompoundIndex(name = "donante_fecha_idx", def = "{'donanteId': 1, 'fechaRegistro': -1}")
public class Donacion {

    @Id
    private String id;

    /** Codigo de seguimiento visible para el usuario, ej. DON-261013-K7P2QX. */
    @Indexed(unique = true)
    private String codigo;

    @Indexed
    private String donanteId;
    private String donanteNombre;
    private String donanteCorreo;

    private List<ItemDonacion> items = new ArrayList<>();
    private int totalUnidades;

    @Indexed
    private EstadoDonacion estado;

    /** La "ruta" de la donacion, en orden cronologico. */
    private List<EventoSeguimiento> seguimiento = new ArrayList<>();

    @Indexed
    private Instant fechaRegistro;
    private Instant fechaRecepcion;
    private Instant fechaAlmacenamiento;
    private String ubicacionAlmacen;

    private String registradaPor;
    private String observaciones;

    private Confirmacion confirmacion;

    /** Codigo para verificar publicamente el certificado (SCAMPER C1). */
    @Indexed(unique = true, sparse = true)
    private String codigoCertificado;

    private Anulacion anulacion;

    /** Control de concurrencia optimista: evita que dos cambios simultaneos se pisen. */
    @Version
    private Long version;

    public void agregarEvento(EventoSeguimiento evento) {
        seguimiento.add(evento);
    }

    public void recalcularTotal() {
        totalUnidades = items.stream().mapToInt(ItemDonacion::getCantidad).sum();
    }
}
