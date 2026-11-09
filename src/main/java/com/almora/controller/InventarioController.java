package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.inventario.AlertaVencimientoResponse;
import com.almora.dto.inventario.InventarioProductoResponse;
import com.almora.dto.inventario.LoteResponse;
import com.almora.dto.inventario.ResumenSemaforoResponse;
import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.EstadoLote;
import com.almora.model.inventario.Semaforo;
import com.almora.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Inventario por lotes con semaforo FEFO. Solo administradores (RNF-S01). */
@Tag(name = "5. Inventario FEFO")
@Validated
@RestController
@RequestMapping("/api/inventario")
@PreAuthorize(Permisos.ADMINISTRATIVO)
public class InventarioController {

    private final InventarioService inventario;

    public InventarioController(InventarioService inventario) {
        this.inventario = inventario;
    }

    @Operation(summary = "Existencias por producto con filtros por tipo y vencimiento (HU09)")
    @GetMapping
    public List<InventarioProductoResponse> resumen(
            @RequestParam(required = false) CategoriaProducto categoria,
            @RequestParam(required = false) Boolean perecedero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceAntesDe,
            @RequestParam(required = false) Semaforo semaforo) {
        return inventario.resumenPorProducto(categoria, perecedero, venceAntesDe, semaforo);
    }

    @Operation(summary = "Lotes en orden FEFO (el que vence primero arriba), con semaforo")
    @GetMapping("/lotes")
    public Pagina<LoteResponse> lotes(
            @RequestParam(required = false) String productoId,
            @RequestParam(required = false) EstadoLote estado,
            @RequestParam(required = false) CategoriaProducto categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceAntesDe,
            @RequestParam(required = false) Semaforo semaforo,
            @RequestParam(required = false) String donacionId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int tamano) {
        return inventario.lotes(productoId, estado, categoria, venceAntesDe, semaforo, donacionId, pagina, tamano);
    }

    @Operation(summary = "Alerta de productos proximos a vencer, ordenados por fecha (HU10)")
    @GetMapping("/alertas")
    public AlertaVencimientoResponse alertas(@RequestParam(required = false) @Min(0) @Max(365) Integer dias) {
        return inventario.alertas(dias);
    }

    @Operation(summary = "Cuantos lotes hay en verde, amarillo y rojo (SCAMPER A1)")
    @GetMapping("/semaforo")
    public ResumenSemaforoResponse semaforo() {
        return inventario.resumenSemaforo();
    }
}
