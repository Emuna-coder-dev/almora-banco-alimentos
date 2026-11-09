package com.almora.repository;

import com.almora.model.entrega.Fundacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FundacionRepository extends MongoRepository<Fundacion, String> {

    boolean existsByNit(String nit);

    Optional<Fundacion> findByNit(String nit);

    long countByActivaTrue();
}
