package com.almora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Habilita transacciones de MongoDB (Atlas siempre funciona como replica set, que las soporta).
 * Con @Transactional, registrar una donacion y crear sus lotes ocurre "todo o nada" (RNF-C01).
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
