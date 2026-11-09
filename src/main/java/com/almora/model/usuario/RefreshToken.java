package com.almora.model.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Token de refresco (sesion larga). Se guarda SOLO su hash SHA-256, nunca el valor real.
 * Cada uso lo rota (se revoca y se emite otro de la misma "familia").
 * Si alguien reutiliza uno ya revocado, se revoca toda la familia (deteccion de robo).
 * MongoDB borra los vencidos automaticamente gracias al indice TTL.
 */
@Getter
@Setter
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String usuarioId;

    @Indexed
    private String familia;

    @Indexed(expireAfter = "0s")
    private Instant expiraEn;

    private boolean revocado;
    private Instant creadoEn;
    private String ip;
    private String userAgent;
}
