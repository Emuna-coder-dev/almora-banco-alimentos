package com.almora.model.auditoria;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de donaciones generado y guardado para consulta posterior (HU11, RNF-C01).
 * Es una "foto" de los datos en el momento de generarse; exportarlo no altera nada (HU12).
 */
@Getter
@Setter
@Document(collection = "reportes")
public class Reporte {

    @Id
    private String id;

    @Indexed
    private Instant fechaGeneracion;
    private String generadoPor;

    private LocalDate desde;
    private LocalDate hasta;
    private String donanteId;
    private String donanteNombre;

    private List<Fila> filas = new ArrayList<>();

    private long totalDonaciones;
    private long totalUnidades;
    /** Se usan listas (no Map) porque MongoDB no acepta puntos en las claves y un producto puede tenerlos. */
    private List<Total> unidadesPorEstado = new ArrayList<>();
    private List<Total> unidadesPorProducto = new ArrayList<>();

    public record Total(String clave, long unidades) {}

    @Getter
    @Setter
    public static class Fila {
        private String codigoDonacion;
        private Instant fecha;
        private String donante;
        private String producto;
        private int cantidad;
        private String unidad;
        private String estado;
    }
}
