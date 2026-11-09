package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.entrega.CambioEstadoEntregaRequest;
import com.almora.dto.entrega.EntregaRequest;
import com.almora.dto.entrega.EntregaResponse;
import com.almora.dto.entrega.PrevisualizacionEntregaResponse;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Entregas a fundaciones con asignacion FEFO. Solo administradores. */
@Tag(name = "8. Entregas FEFO")
@Validated
@RestController
@RequestMapping("/api/entregas")
@PreAuthorize(Permisos.ADMINISTRATIVO)
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @Operation(summary = "Ver la propuesta FEFO (de que lotes sale cada producto) sin reservar nada")
    @PostMapping("/previsualizar")
    public PrevisualizacionEntregaResponse previsualizar(@Valid @RequestBody EntregaRequest req) {
        return entregaService.previsualizar(req);
    }

    @Operation(summary = "Crear entrega: reserva los lotes que vencen primero (FEFO)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaResponse crear(@Valid @RequestBody EntregaRequest req) {
        return entregaService.crear(req);
    }

    @Operation(summary = "Listar entregas")
    @GetMapping
    public Pagina<EntregaResponse> listar(
            @RequestParam(required = false) EstadoEntrega estado,
            @RequestParam(required = false) String fundacionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return entregaService.listar(estado, fundacionId, desde, hasta, pagina, tamano);
    }

    @Operation(summary = "Ver entrega con su ruta de seguimiento")
    @GetMapping("/{id}")
    public EntregaResponse obtener(@PathVariable String id) {
        return entregaService.obtener(id);
    }

    @Operation(summary = "Avanzar entrega: EN_RUTA, ENTREGADA o CANCELADA")
    @PatchMapping("/{id}/estado")
    public EntregaResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoEntregaRequest req) {
        return entregaService.cambiarEstado(id, req);
    }
}
