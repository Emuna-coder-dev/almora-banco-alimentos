package com.almora.controller;

import com.almora.config.AlmoraProperties;
import com.almora.dto.auth.AuthResponse;
import com.almora.dto.auth.LoginRequest;
import com.almora.dto.auth.RegistroDonanteRequest;
import com.almora.dto.comun.MensajeResponse;
import com.almora.dto.usuario.UsuarioResponse;
import com.almora.security.SecurityUtils;
import com.almora.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Inicio de sesion, registro de donantes, renovacion y cierre de sesion. Rutas publicas.
 * El token de refresco se maneja SOLO con una cookie HttpOnly + SameSite=Strict limitada a /api/auth.
 */
@Tag(name = "1. Autenticacion")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String COOKIE = "almora_refresh";

    private final AuthService authService;
    private final AlmoraProperties propiedades;

    public AuthController(AuthService authService, AlmoraProperties propiedades) {
        this.authService = authService;
        this.propiedades = propiedades;
    }

    @Operation(summary = "Registro publico de donantes (HU01/HU03)")
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody RegistroDonanteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarDonante(req));
    }

    @Operation(summary = "Iniciar sesion: devuelve el token de acceso y pone la cookie de refresco")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        AuthService.Sesion s = authService.login(req, SecurityUtils.ipCliente(http), http.getHeader(HttpHeaders.USER_AGENT));
        return conCookie(s);
    }

    @Operation(summary = "Renovar el token de acceso usando la cookie de refresco (rotativa)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String refresco,
                                                HttpServletRequest http) {
        AuthService.Sesion s = authService.refrescar(refresco, SecurityUtils.ipCliente(http), http.getHeader(HttpHeaders.USER_AGENT));
        return conCookie(s);
    }

    @Operation(summary = "Cerrar sesion: revoca el token de refresco y borra la cookie")
    @PostMapping("/logout")
    public ResponseEntity<MensajeResponse> logout(@CookieValue(name = COOKIE, required = false) String refresco) {
        authService.logout(refresco);
        ResponseCookie borrar = cookie("", Duration.ZERO);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, borrar.toString())
                .body(new MensajeResponse("Sesion cerrada"));
    }

    private ResponseEntity<AuthResponse> conCookie(AuthService.Sesion s) {
        ResponseCookie c = cookie(s.refreshToken(), Duration.ofSeconds(s.segundosRefresco()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, c.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(s.respuesta());
    }

    private ResponseCookie cookie(String valor, Duration duracion) {
        return ResponseCookie.from(COOKIE, valor)
                .httpOnly(true)
                .secure(propiedades.jwt().cookieSegura())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(duracion)
                .build();
    }
}
