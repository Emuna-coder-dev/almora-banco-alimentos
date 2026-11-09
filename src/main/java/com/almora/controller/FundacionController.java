package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.entrega.EntregaResponse;
import com.almora.dto.fundacion.CambioEstadoFundacionRequest;
import com.almora.dto.fundacion.FundacionRequest;
import com.almora.dto.fundacion.FundacionResponse;
import com.almora.service.FundacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "7. Fundaciones")
@Validated
@RestController
@RequestMapping("/api/fundaciones")
@PreAuthorize(Permisos.ADMINISTRATIVO)
public class FundacionController {

    private final FundacionService fundacionService;

    public FundacionController(FundacionService fundacionService) {
        this.fundacionService = fundacionService;
    }

    @Operation(summary = "Registrar fundacion receptora")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FundacionResponse crear(@Valid @RequestBody FundacionRequest req) {
        return fundacionService.crear(req);
    }

    @Operation(summary = "Listar fundaciones")
    @GetMapping
    public List<FundacionResponse> listar(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Boolean activa) {
        return fundacionService.listar(q, activa);
    }

    @Operation(summary = "Ver fundacion")
    @GetMapping("/{id}")
    public FundacionResponse obtener(@PathVariable String id) {
        return fundacionService.obtener(id);
    }

    @Operation(summary = "Editar fundacion")
    @PutMapping("/{id}")
    public FundacionResponse actualizar(@PathVariable String id, @Valid @RequestBody FundacionRequest req) {
        return fundacionService.actualizar(id, req);
    }

    @Operation(summary = "Activar o desactivar fundacion")
    @PatchMapping("/{id}/estado")
    public FundacionResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoFundacionRequest req) {
        return fundacionService.cambiarEstado(id, req.activa());
    }

    @Operation(summary = "Historial de entregas de la fundacion")
    @GetMapping("/{id}/entregas")
    public Pagina<EntregaResponse> historial(@PathVariable String id,
                                             @RequestParam(defaultValue = "0") @Min(0) int pagina,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return fundacionService.historial(id, pagina, tamano);
    }
}
