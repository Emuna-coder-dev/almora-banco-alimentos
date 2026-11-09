package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.auditoria.RegistroAuditoria;
import com.almora.service.AuditoriaService;
import com.almora.service.RespaldoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Auditoria (RNF-AU01) y respaldos (RNF-BD01). */
@Tag(name = "10. Auditoria y respaldos")
@Validated
@RestController
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final RespaldoService respaldoService;

    public AuditoriaController(AuditoriaService auditoriaService, RespaldoService respaldoService) {
        this.auditoriaService = auditoriaService;
        this.respaldoService = respaldoService;
    }

    @Operation(summary = "Consultar la bitacora de auditoria (incluye las donaciones anuladas: accion=ANULAR)")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @GetMapping("/api/auditoria")
    public Pagina<RegistroAuditoria> buscar(
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int tamano) {
        return auditoriaService.buscar(accion, entidad, usuario, entidadId, desde, hasta, pagina, tamano);
    }

    @Operation(summary = "Crear un respaldo ahora (solo superadministrador)")
    @PreAuthorize(Permisos.SUPERADMIN)
    @PostMapping("/api/respaldos")
    @ResponseStatus(HttpStatus.CREATED)
    public RespaldoService.InfoRespaldo respaldar() {
        return respaldoService.crearRespaldo();
    }

    @Operation(summary = "Listar respaldos guardados (solo superadministrador)")
    @PreAuthorize(Permisos.SUPERADMIN)
    @GetMapping("/api/respaldos")
    public List<RespaldoService.InfoRespaldo> respaldos() {
        return respaldoService.listar();
    }
}
