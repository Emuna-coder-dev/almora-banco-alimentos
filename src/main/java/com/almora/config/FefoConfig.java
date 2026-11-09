package com.almora.config;

import com.almora.fefo.CalculadoraSemaforo;
import com.almora.fefo.FefoAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el algoritmo FEFO y el semaforo como beans, con los umbrales de application.properties. */
@Configuration
public class FefoConfig {

    @Bean
    public FefoAllocator fefoAllocator() {
        return new FefoAllocator();
    }

    @Bean
    public CalculadoraSemaforo calculadoraSemaforo(AlmoraProperties propiedades) {
        return new CalculadoraSemaforo(propiedades.fefo().diasAmarillo(), propiedades.fefo().diasRojo());
    }
}
