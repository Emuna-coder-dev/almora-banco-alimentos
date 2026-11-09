package com.almora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;
import java.util.List;

/**
 * Todas las propiedades "almora.*" de application.properties en un solo lugar.
 * Los umbrales del semaforo FEFO se definen aqui (SCAMPER A1: "en un solo lugar de configuracion").
 */
@ConfigurationProperties(prefix = "almora")
public record AlmoraProperties(
        Jwt jwt,
        Seguridad seguridad,
        Cors cors,
        Fefo fefo,
        SuperAdmin superadmin,
        Mail mail,
        Respaldo respaldo,
        Rendimiento rendimiento,
        String zonaHoraria,
        boolean datosDemo
) {
    public record Jwt(String secreto, String emisor, long minutosAcceso, long diasRefresco, boolean cookieSegura) {}

    public record Seguridad(int maxIntentos, long minutosBloqueo) {}

    public record Cors(List<String> origenesPermitidos) {}

    public record Fefo(int diasAmarillo, int diasRojo, int diasAlerta, String cronVencimientos) {}

    public record SuperAdmin(String correo, String password, String nombre) {}

    public record Mail(boolean habilitado, String remitente) {}

    public record Respaldo(boolean habilitado, String carpeta, String cron, int conservar) {}

    public record Rendimiento(long limiteMs) {}

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria == null || zonaHoraria.isBlank() ? "America/Bogota" : zonaHoraria);
    }
}
