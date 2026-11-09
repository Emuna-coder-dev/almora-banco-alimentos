package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.donacion.ActualizarDonacionRequest;
import com.almora.dto.donacion.AnulacionRequest;
import com.almora.dto.donacion.CambioEstadoDonacionRequest;
import com.almora.dto.donacion.DonacionRequest;
import com.almora.dto.donacion.DonacionResponse;
import com.almora.dto.donacion.SeguimientoResponse;
import com.almora.model.donacion.Confirmacion;
import com.almora.model.donacion.EstadoDonacion;
import com.almora.service.CertificacionService;
import com.almora.service.CorreoService;
import com.almora.service.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;

/**
 * Donaciones. Los tres roles entran aqui, pero cada uno con su alcance:
 *  - DONANTE: registra y ve SOLO sus donaciones, su seguimiento y descarga su certificado.
 *  - ADMIN / SUPERADMIN: registran a nombre de donantes, cambian estados, anulan y ven todas.
 */
@Tag(name = "6. Donaciones y seguimiento")
@Validated
@RestController
@RequestMapping("/api/donaciones")
public class DonacionController {

    private final DonacionService donacionService;
    private final CertificacionService certificacionService;
    private final CorreoService correoService;

    public DonacionController(DonacionService donacionService, CertificacionService certificacionService,
                              CorreoService correoService) {
        this.donacionService = donacionService;
        this.certificacionService = certificacionService;
        this.correoService = correoService;
    }

    @Operation(summary = "Registrar donacion con uno o varios productos (HU05)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonacionResponse registrar(@Valid @RequestBody DonacionRequest req) {
        return donacionService.registrar(req);
    }

    @Operation(summary = "Listar donaciones (el donante solo ve las suyas). Filtros: estado, donante, fechas, codigo, confirmacion")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping
    public Pagina<DonacionResponse> listar(
            @RequestParam(required = false) EstadoDonacion estado,
            @RequestParam(required = false) String donanteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Confirmacion.Estado confirmacion,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return donacionService.listar(estado, donanteId, desde, hasta, codigo, confirmacion, pagina, tamano);
    }

    @Operation(summary = "Ver una donacion")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping("/{id}")
    public DonacionResponse obtener(@PathVariable String id) {
        return donacionService.obtener(id);
    }

    @Operation(summary = "Seguimiento de la donacion tipo guia de mensajeria (los tres roles)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping("/{id}/seguimiento")
    public SeguimientoResponse seguimiento(@PathVariable String id) {
        return donacionService.seguimientoPorId(id);
    }

    @Operation(summary = "Seguimiento por codigo (ej. DON-261013-K7P2QX)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping("/codigo/{codigo}/seguimiento")
    public SeguimientoResponse seguimientoPorCodigo(@PathVariable String codigo) {
        return donacionService.seguimientoPorCodigo(codigo);
    }

    @Operation(summary = "Descargar certificado de donacion en PDF (SCAMPER C1)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @GetMapping("/{id}/certificado")
    public ResponseEntity<byte[]> certificado(@PathVariable String id, HttpServletRequest http) {
        String base = ServletUriComponentsBuilder.fromContextPath(http).build().toUriString();
        byte[] pdf = certificacionService.certificado(id, base);
        return Archivos.descargar(pdf, "certificado-donacion-" + id + ".pdf", MediaType.APPLICATION_PDF);
    }

    @Operation(summary = "Editar productos de una donacion PENDIENTE (RF007)")
    @PreAuthorize(Permisos.CUALQUIERA)
    @PutMapping("/{id}")
    public DonacionResponse actualizar(@PathVariable String id, @Valid @RequestBody ActualizarDonacionRequest req) {
        return donacionService.actualizar(id, req);
    }

    @Operation(summary = "Anular donacion con motivo (RF006/HU07). El donante solo puede anular las suyas pendientes")
    @PreAuthorize(Permisos.CUALQUIERA)
    @PostMapping("/{id}/anular")
    public DonacionResponse anular(@PathVariable String id, @Valid @RequestBody AnulacionRequest req) {
        return donacionService.anular(id, req.motivo());
    }

    @Operation(summary = "Cambiar estado: PENDIENTE->RECIBIDA (entra al inventario) o RECIBIDA->ALMACENADA (HU08)")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @PatchMapping("/{id}/estado")
    public DonacionResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoDonacionRequest req) {
        return donacionService.cambiarEstado(id, req.estado(), req.ubicacionAlmacen(), req.observacion());
    }

    @Operation(summary = "Reenviar el correo de confirmacion al donante (HU06)")
    @PreAuthorize(Permisos.ADMINISTRATIVO)
    @PostMapping("/{id}/reenviar-confirmacion")
    public Confirmacion reenviarConfirmacion(@PathVariable String id) {
        donacionService.buscar(id);
        return correoService.enviarConfirmacion(id);
    }
}
