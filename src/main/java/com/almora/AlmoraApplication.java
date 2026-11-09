package com.almora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la API del banco de alimentos ALMORA.
 *
 * - @EnableScheduling: tarea diaria que marca los lotes vencidos (HU10).
 * - @EnableAsync: el correo de confirmacion se envia en segundo plano (HU06, RNF-R01).
 * - @EnableMongoAuditing: llena fechaCreacion / fechaActualizacion automaticamente (HU02).
 * - El login lo hace AuthService con JWT (ver security/SecurityConfig).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
@EnableMongoAuditing
public class AlmoraApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlmoraApplication.class, args);
    }
}
