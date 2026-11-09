package com.almora.security;

import com.almora.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UsuarioAutenticado> actualOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static UsuarioAutenticado actual() {
        return actualOpcional().orElseThrow(() -> ApiException.noAutorizado("Debes iniciar sesion"));
    }

    public static Optional<HttpServletRequest> peticionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }

    /** IP del cliente. Si hay un proxy (Render, Railway, Nginx), viene en X-Forwarded-For. */
    public static String ipCliente(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        String reenviada = req.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
