package com.almora.repository;

import com.almora.model.auditoria.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReporteRepository extends MongoRepository<Reporte, String> {

    Page<Reporte> findAllByOrderByFechaGeneracionDesc(Pageable pageable);
}
