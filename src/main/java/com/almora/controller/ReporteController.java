package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.reporte.IndicadoresResponse;
import com.almora.dto.reporte.ReporteRequest;
import com.almora.dto.reporte.ReporteResumenResponse;
import com.almora.model.auditoria.Reporte;
import com.almora.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9. Reportes e indicadores")
@Validated
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize(Permisos.ADMINISTRATIVO)
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Generar y guardar un reporte por rango de fechas y donante (HU11)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reporte generar(@Valid @RequestBody ReporteRequest req) {
        return reporteService.generar(req);
    }

    @Operation(summary = "Reportes generados anteriormente")
    @GetMapping
    public Pagina<ReporteResumenResponse> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return reporteService.listar(pagina, tamano);
    }

    @Operation(summary = "Ver un reporte guardado")
    @GetMapping("/{id}")
    public Reporte obtener(@PathVariable String id) {
        return reporteService.obtener(id);
    }

    @Operation(summary = "Exportar reporte a PDF (HU12)")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String id) {
        return Archivos.descargar(reporteService.exportarPdf(id), "reporte-donaciones-" + id + ".pdf", MediaType.APPLICATION_PDF);
    }

    @Operation(summary = "Exportar reporte a Excel (HU12)")
    @GetMapping("/{id}/excel")
    public ResponseEntity<byte[]> excel(@PathVariable String id) {
        return Archivos.descargar(reporteService.exportarExcel(id), "reporte-donaciones-" + id + ".xlsx", Archivos.EXCEL);
    }

    @Operation(summary = "Tablero de indicadores: % entregado antes de vencer, stock, semaforo (SCAMPER S1)")
    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores() {
        return reporteService.indicadores();
    }
}
