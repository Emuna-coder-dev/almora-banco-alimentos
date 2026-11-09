package com.almora.repository;

import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.EstadoDonacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DonacionRepository extends MongoRepository<Donacion, String> {

    Optional<Donacion> findByCodigo(String codigo);

    Optional<Donacion> findByCodigoCertificado(String codigoCertificado);

    boolean existsByCodigo(String codigo);

    List<Donacion> findByDonanteId(String donanteId);

    long countByDonanteId(String donanteId);

    long countByEstado(EstadoDonacion estado);
}
