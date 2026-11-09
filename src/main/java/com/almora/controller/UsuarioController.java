package com.almora.controller;

import com.almora.dto.comun.Pagina;
import com.almora.dto.donacion.DonacionResponse;
import com.almora.dto.usuario.ActualizarUsuarioRequest;
import com.almora.dto.usuario.CambioEstadoUsuarioRequest;
import com.almora.dto.usuario.CambioRolRequest;
import com.almora.dto.usuario.CrearAdminRequest;
import com.almora.dto.usuario.CrearDonanteRequest;
import com.almora.dto.usuario.CuentaCreadaResponse;
import com.almora.dto.usuario.UsuarioResponse;
import com.almora.service.DonacionService;
import com.almora.service.UsuarioService;
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

/**
 * Gestion de cuentas.
 *  - /administradores: solo SUPERADMIN (RF001 / HU01).
 *  - /donantes: ADMIN y SUPERADMIN (RF003 / HU03, HU04).
 *  - /{id}: editar, activar/desactivar, restablecer clave (RF002 / HU02) -> la clase del actor decide si puede.
 *  - /{id}/rol: solo SUPERADMIN.
 */
@Tag(name = "3. Usuarios")
@Validated
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize(Permisos.ADMINISTRATIVO)
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final DonacionService donacionService;

    public UsuarioController(UsuarioService usuarioService, DonacionService donacionService) {
        this.usuarioService = usuarioService;
        this.donacionService = donacionService;
    }

    @Operation(summary = "Crear administrador (solo superadministrador)")
    @PreAuthorize(Permisos.SUPERADMIN)
    @PostMapping("/administradores")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaCreadaResponse crearAdministrador(@Valid @RequestBody CrearAdminRequest req) {
        return usuarioService.crearAdministrador(req);
    }

    @Operation(summary = "Listar administradores (solo superadministrador)")
    @PreAuthorize(Permisos.SUPERADMIN)
    @GetMapping("/administradores")
    public Pagina<UsuarioResponse> listarAdministradores(@RequestParam(defaultValue = "0") @Min(0) int pagina,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return usuarioService.listarAdministradores(pagina, tamano);
    }

    @Operation(summary = "Registrar donante (HU03)")
    @PostMapping("/donantes")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaCreadaResponse crearDonante(@Valid @RequestBody CrearDonanteRequest req) {
        return usuarioService.crearDonante(req);
    }

    @Operation(summary = "Buscar donantes por nombre, identificacion o correo (HU04)")
    @GetMapping("/donantes")
    public Pagina<UsuarioResponse> buscarDonantes(@RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") @Min(0) int pagina,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return usuarioService.buscarDonantes(q, pagina, tamano);
    }

    @Operation(summary = "Ver una cuenta")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable String id) {
        return usuarioService.obtener(id);
    }

    @Operation(summary = "Historial de donaciones de un donante (HU04: se conserva al editarlo)")
    @GetMapping("/{id}/donaciones")
    public Pagina<DonacionResponse> donacionesDe(@PathVariable String id,
                                                 @RequestParam(defaultValue = "0") @Min(0) int pagina,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        usuarioService.buscarDonante(id);
        return donacionService.listar(null, id, null, null, null, null, pagina, tamano);
    }

    @Operation(summary = "Editar una cuenta (HU02, HU04). Admin: solo donantes. Superadmin: donantes y administradores")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable String id, @Valid @RequestBody ActualizarUsuarioRequest req) {
        return usuarioService.actualizar(id, req);
    }

    @Operation(summary = "Cambiar el rol de una cuenta entre ADMIN y DONANTE (solo superadministrador)")
    @PreAuthorize(Permisos.SUPERADMIN)
    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable String id, @Valid @RequestBody CambioRolRequest req) {
        return usuarioService.cambiarRol(id, req.rol());
    }

    @Operation(summary = "Activar o desactivar una cuenta")
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoUsuarioRequest req) {
        return usuarioService.cambiarEstado(id, req.activo());
    }

    @Operation(summary = "Restablecer la contrasena (genera una temporal)")
    @PostMapping("/{id}/restablecer-password")
    public CuentaCreadaResponse restablecerPassword(@PathVariable String id) {
        return usuarioService.restablecerPassword(id);
    }
}
