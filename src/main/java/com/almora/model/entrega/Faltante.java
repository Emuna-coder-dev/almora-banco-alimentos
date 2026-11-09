package com.almora.model.entrega;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que no se pudo cubrir con el inventario (paso 7 del algoritmo FEFO). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Faltante {
    private String productoId;
    private String nombreProducto;
    private int solicitado;
    private int cubierto;
    private int noCubierto;
}
