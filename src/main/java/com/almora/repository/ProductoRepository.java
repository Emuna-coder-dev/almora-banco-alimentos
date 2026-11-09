package com.almora.repository;

import com.almora.model.inventario.Producto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends MongoRepository<Producto, String> {

    Optional<Producto> findByNombreNormalizado(String nombreNormalizado);

    List<Producto> findByActivoTrue();
}
