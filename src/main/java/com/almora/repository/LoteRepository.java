package com.almora.repository;

import com.almora.model.inventario.EstadoLote;
import com.almora.model.inventario.Lote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface LoteRepository extends MongoRepository<Lote, String> {

    List<Lote> findByDonacionId(String donacionId);

    List<Lote> findByDonacionIdIn(Collection<String> donacionIds);

    /** Lotes candidatos para el algoritmo FEFO (se filtran y ordenan en FefoAllocator). */
    List<Lote> findByProductoIdAndEstado(String productoId, EstadoLote estado);

    boolean existsByProductoId(String productoId);

    boolean existsByCodigo(String codigo);
}
