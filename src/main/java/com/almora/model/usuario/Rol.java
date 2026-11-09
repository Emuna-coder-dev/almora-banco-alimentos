package com.almora.model.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * CLASE ROL: clase base (abstracta) de la que heredan todos los tipos de cuenta.
 * Guarda lo que comparten los tres roles: nombre, correo, contrasena, telefono, estado y datos de sesion.
 *
 * Jerarquia (herencia):
 *
 *               Rol  (abstracta)
 *              /    \
 *        Donante    Admin
 *                     |
 *                 SuperAdmin
 *
 *  - Donante:    ve sus donaciones, su seguimiento y descarga sus certificados en PDF.
 *  - Admin:      gestiona donantes, productos, donaciones, inventario FEFO, fundaciones, entregas y reportes.
 *  - SuperAdmin: hereda TODO lo de Admin y ademas crea y administra a los administradores.
 *
 * Las tres subclases se guardan en UNA sola coleccion "usuarios" (SCAMPER C2: sin documentos duplicados).
 * Spring Data agrega el campo "_class" (donante / admin / superadmin) para saber que subclase crear al leer.
 *
 * Los permisos se resuelven con POLIMORFISMO: cada subclase sobrescribe puedeGestionar(...).
 * El campo "tipo" (enum TipoRol) lo fija el constructor de cada subclase; nunca lo elige quien se registra.
 */
@Getter
@Setter
@Document(collection = "usuarios")
public abstract class Rol {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String correo;

    /** Hash BCrypt. Nunca se devuelve en las respuestas de la API (se usan DTOs). */
    private String password;

    /** SUPERADMIN, ADMIN o DONANTE. Solo lo asigna el constructor de la subclase. */
    @Indexed
    @Setter(lombok.AccessLevel.NONE)
    private TipoRol rol;

    private String telefono;
    private boolean activo = true;

    /** true cuando un administrador creo la cuenta con una clave temporal: se pide cambiarla. */
    private boolean debeCambiarPassword;

    // --- Proteccion contra fuerza bruta ---
    private int intentosFallidos;
    private Instant bloqueadoHasta;

    /** Los JWT emitidos antes de esta fecha dejan de ser validos (cambio de clave, de rol o desactivacion). */
    private Instant sesionesInvalidadasEn;
    private Instant ultimoAcceso;

    @CreatedDate
    private Instant fechaCreacion;

    /** Fecha de la ultima actualizacion (HU02: "se registra la fecha de actualizacion"). */
    @LastModifiedDate
    private Instant fechaActualizacion;

    protected Rol(TipoRol rol) {
        this.rol = rol;
    }

    /** Indica si esta cuenta puede editar, activar o desactivar a "otro". Cada subclase decide. */
    public abstract boolean puedeGestionar(Rol otro);

    /** Solo el superadministrador crea administradores (RF001 / HU01). */
    public boolean puedeGestionarAdministradores() {
        return false;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Copia los datos comunes a otra cuenta (se usa al cambiar el rol de una cuenta: HU02). */
    public void copiarDatosComunesA(Rol destino) {
        destino.setId(id);
        destino.setNombre(nombre);
        destino.setCorreo(correo);
        destino.setPassword(password);
        destino.setTelefono(telefono);
        destino.setActivo(activo);
        destino.setDebeCambiarPassword(debeCambiarPassword);
        destino.setUltimoAcceso(ultimoAcceso);
        destino.setFechaCreacion(fechaCreacion);
    }
}
