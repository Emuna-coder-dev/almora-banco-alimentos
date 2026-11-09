package com.almora.repository;

import com.almora.model.entrega.Entrega;
import com.almora.model.entrega.EstadoEntrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface EntregaRepository extends MongoRepository<Entrega, String> {

    /** Todas las entregas que llevan productos de una donacion (para su seguimiento). */
    List<Entrega> findByItemsDonacionId(String donacionId);

    List<Entrega> findByItemsDonacionIdIn(Collection<String> donacionIds);

    Page<Entrega> findByFundacionIdOrderByFechaCreacionDesc(String fundacionId, Pageable pageable);

    List<Entrega> findByEstado(EstadoEntrega estado);

    long countByEstado(EstadoEntrega estado);

    boolean existsByCodigo(String codigo);
}
