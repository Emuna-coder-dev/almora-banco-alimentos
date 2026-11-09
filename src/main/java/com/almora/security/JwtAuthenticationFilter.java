package com.almora.security;

import com.almora.model.usuario.Rol;
import com.almora.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Se ejecuta en cada peticion. Si trae "Authorization: Bearer <token>":
 *  1. Valida firma, emisor y expiracion del JWT.
 *  2. Busca al usuario en la base de datos (el rol vigente es el de la BD, no el del token).
 *  3. Rechaza si la cuenta esta inactiva o si sus sesiones se invalidaron despues de emitir el token
 *     (por cambio de clave, cambio de rol o desactivacion).
 *  4. Si todo esta bien, registra al usuario como autenticado para esta peticion.
 * Si algo falla no autentica; Spring Security responde 401 en las rutas protegidas.
 *
 * No se marca como @Component a proposito: se registra solo dentro de SecurityConfig
 * (si fuera @Component, Spring Boot lo agregaria otra vez como filtro general).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
            chain.doFilter(request, response);
            return;
        }

        String token = encabezado.substring(PREFIJO.length()).trim();
        try {
            Claims claims = jwtService.validar(token);
            Rol usuario = usuarioRepository.findById(claims.getSubject()).orElse(null);

            if (usuario != null && usuario.isActivo() && sesionVigente(usuario, claims)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UsuarioAutenticado principal = new UsuarioAutenticado(
                        usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getRol());
                UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority(usuario.getRol().autoridad())));
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token rechazado: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private boolean sesionVigente(Rol usuario, Claims claims) {
        Instant invalidadas = usuario.getSesionesInvalidadasEn();
        if (invalidadas == null) {
            return true;
        }
        Instant emitido = claims.getIssuedAt().toInstant();
        return !emitido.isBefore(invalidadas.truncatedTo(ChronoUnit.SECONDS));
    }
}
