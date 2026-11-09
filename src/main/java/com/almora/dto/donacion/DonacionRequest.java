package com.almora.dto.donacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Registro de una donacion con uno o varios productos (RF004 / HU05, SCAMPER M2).
 * donanteId: lo envia un administrador que registra a nombre de un donante.
 * Si quien registra es el propio donante, se ignora y se usa su cuenta.
 */
public record DonacionRequest(
        String donanteId,
        @NotEmpty(message = "La donacion debe tener al menos un producto")
        @Size(max = 50, message = "Maximo 50 productos por donacion") List<@Valid ItemDonacionRequest> items,
        @Size(max = 500) String observaciones,
        @Size(max = 120) String ubicacionAlmacen
) {
}
