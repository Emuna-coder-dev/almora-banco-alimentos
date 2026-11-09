package com.almora.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion interactiva en http://localhost:8080/swagger-ui.html
 * El boton "Authorize" permite pegar el token JWT para probar los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI almoraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ALMORA - Banco de alimentos")
                        .version("1.0.0")
                        .description("API para gestionar donantes, donaciones, inventario por lotes con asignacion FEFO, "
                                + "entregas a fundaciones, seguimiento, certificados, reportes y auditoria."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
