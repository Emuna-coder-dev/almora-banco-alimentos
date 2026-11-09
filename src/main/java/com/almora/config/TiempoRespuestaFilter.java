package com.almora.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Mide cuanto tarda cada peticion a la API (RNF-R01 y RNF-R02: maximo 3 segundos).
 * - Agrega el encabezado "X-Tiempo-Respuesta-ms" (se ve en el navegador o en Postman).
 * - Si pasa del limite configurado, deja una advertencia en el log para revisarla.
 */
@Component
public class TiempoRespuestaFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TiempoRespuestaFilter.class);
    private final long limiteMs;

    public TiempoRespuestaFilter(AlmoraProperties propiedades) {
        this.limiteMs = propiedades.rendimiento() == null ? 3000 : propiedades.rendimiento().limiteMs();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        // El cuerpo se guarda un momento en memoria para poder agregar el encabezado con el tiempo al final
        ContentCachingResponseWrapper envoltura = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, envoltura);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            envoltura.setHeader("X-Tiempo-Respuesta-ms", String.valueOf(ms));
            envoltura.copyBodyToResponse();
            if (ms > limiteMs) {
                log.warn("RENDIMIENTO: {} {} tardo {} ms (limite {} ms)", request.getMethod(), request.getRequestURI(), ms, limiteMs);
            }
        }
    }
}
