package com.almora.model.inventario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Catalogo de productos (SCAMPER S2). El producto NO guarda cantidades ni fechas:
 * eso vive en cada Lote. El stock de un producto es la suma de sus lotes disponibles.
 */
@Getter
@Setter
@Document(collection = "productos")
public class Producto {

    @Id
    private String id;

    private String nombre;

    /** Nombre en minusculas y sin espacios extra: evita "Arroz" y "arroz " duplicados. */
    @Indexed(unique = true)
    private String nombreNormalizado;

    @Indexed
    private CategoriaProducto categoria;

    /** Perecedero = exige fecha de vencimiento al donarlo (HU05). */
    private boolean perecedero;

    private UnidadMedida unidadMedida;
    private String descripcion;
    private boolean activo = true;

    @CreatedDate
    private Instant fechaCreacion;

    @LastModifiedDate
    private Instant fechaActualizacion;

    public static String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
