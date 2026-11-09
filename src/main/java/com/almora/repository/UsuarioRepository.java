package com.almora.repository;

import com.almora.model.usuario.TipoRol;
import com.almora.model.usuario.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.Optional;

/**
 * Un solo repositorio para los tres roles: MongoDB devuelve el objeto de la subclase correcta
 * (Donante, Admin o SuperAdmin) gracias al campo "_class".
 */
public interface UsuarioRepository extends MongoRepository<Rol, String> {

    Optional<Rol> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRol(TipoRol rol);

    long countByRolAndActivoTrue(TipoRol rol);

    Page<Rol> findByRolIn(Collection<TipoRol> roles, Pageable pageable);

    /** Busqueda de donantes por nombre (parcial, sin importar mayusculas) o por identificacion exacta (HU04). */
    @Query("{ 'rol': 'DONANTE', $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'identificacion': ?1 }, { 'correo': ?1 } ] }")
    Page<Rol> buscarDonantes(String patronNombre, String textoExacto, Pageable pageable);

    @Query("{ 'rol': 'DONANTE', 'identificacion': ?0 }")
    Optional<Rol> findDonanteByIdentificacion(String identificacion);
}
