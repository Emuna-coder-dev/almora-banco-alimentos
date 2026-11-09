package com.almora.controller;

import com.almora.dto.auth.CambioPasswordRequest;
import com.almora.dto.comun.MensajeResponse;
import com.almora.dto.usuario.ActualizarPerfilRequest;
import com.almora.dto.usuario.ImpactoDonanteResponse;
import com.almora.dto.usuario.UsuarioResponse;
import com.almora.security.SecurityUtils;
import com.almora.service.ReporteService;
import com.almora.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que cada usuario hace sobre su propia cuenta (los tres roles). */
@Tag(name = "2. Mi perfil")
@RestController
@RequestMapping("/api/perfil")
@PreAuthorize(Permisos.CUALQUIERA)
public class PerfilController {

    private final UsuarioService usuarioService;
    private final ReporteService reporteService;

    public PerfilController(UsuarioService usuarioService, ReporteService reporteService) {
        this.usuarioService = usuarioService;
        this.reporteService = reporteService;
    }

    @Operation(summary = "Ver mi perfil")
    @GetMapping
    public UsuarioResponse miPerfil() {
        return usuarioService.miPerfil();
    }

    @Operation(summary = "Editar mi perfil (nombre, telefono y, si soy donante, direccion y contacto)")
    @PutMapping
    public UsuarioResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest req) {
        return usuarioService.actualizarPerfil(req);
    }

    @Operation(summary = "Cambiar mi contrasena (cierra todas mis sesiones)")
    @PostMapping("/password")
    public MensajeResponse cambiarPassword(@Valid @RequestBody CambioPasswordRequest req) {
        usuarioService.cambiarPassword(req);
        return new MensajeResponse("Contrasena actualizada. Inicia sesion de nuevo.");
    }

    @Operation(summary = "Mi impacto como donante: unidades entregadas y fundaciones atendidas (SCAMPER P1)")
    @PreAuthorize(Permisos.DONANTE)
    @GetMapping("/impacto")
    public ImpactoDonanteResponse impacto() {
        return reporteService.impacto(SecurityUtils.actual().id());
    }
}
