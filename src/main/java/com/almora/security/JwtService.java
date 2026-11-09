package com.almora.security;

import com.almora.config.AlmoraProperties;
import com.almora.model.usuario.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Emite y valida los tokens de acceso (JWT firmados con HMAC-SHA256).
 *
 * Medidas de seguridad:
 *  - El secreto viene de una variable de entorno y debe tener al menos 256 bits; si no, la app no arranca.
 *  - Vida corta (15 min por defecto). Para seguir conectado se usa el refresh token rotativo.
 *  - Se exige firma valida, emisor correcto y fecha de expiracion (se rechaza el algoritmo "none").
 *  - El token no lleva datos sensibles: solo id, correo y rol.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final AlmoraProperties.Jwt config;

    public JwtService(AlmoraProperties propiedades) {
        this.config = propiedades.jwt();
        if (config == null || config.secreto() == null || config.secreto().isBlank()) {
            throw new IllegalStateException("Falta la variable JWT_SECRET. Generala con: openssl rand -base64 32");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(config.secreto().trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET debe estar en Base64. Generalo con: openssl rand -base64 32");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET es muy corto: debe tener al menos 32 bytes (256 bits)");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
    }

    public String generarTokenAcceso(Rol usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(config.emisor())
                .subject(usuario.getId())
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(config.minutosAcceso(), ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /** Devuelve los datos del token o lanza JwtException si es invalido, esta alterado o expiro. */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(config.emisor())
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long segundosDeVidaAcceso() {
        return config.minutosAcceso() * 60;
    }
}
