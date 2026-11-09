package com.almora.service;

import com.almora.dto.auth.CambioPasswordRequest;
import com.almora.dto.comun.Pagina;
import com.almora.dto.usuario.ActualizarPerfilRequest;
import com.almora.dto.usuario.ActualizarUsuarioRequest;
import com.almora.dto.usuario.CrearAdminRequest;
import com.almora.dto.usuario.CrearDonanteRequest;
import com.almora.dto.usuario.CuentaCreadaResponse;
import com.almora.dto.usuario.UsuarioResponse;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.usuario.Admin;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.Rol;
import com.almora.model.usuario.TipoDonante;
import com.almora.model.usuario.TipoRol;
import com.almora.repository.UsuarioRepository;
import com.almora.security.SecurityUtils;
import com.almora.security.UsuarioAutenticado;
import com.almora.util.ClavesTemporales;
import com.almora.util.Textos;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Gestion de cuentas segun la jerarquia de roles:
 *  - SuperAdmin: crea, edita, activa/desactiva y cambia el rol de administradores y donantes (RF001, HU01, HU02).
 *  - Admin: crea y edita SOLO donantes (RF003, HU03, HU04).
 *  - Cualquiera: ve y edita su propio perfil y cambia su contrasena.
 * La decision "puede gestionar a X" la toma la propia clase del actor (polimorfismo de Rol.puedeGestionar).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder,
                          AuthService authService, AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ crear

    /** Solo el superadministrador crea administradores (RF001 / HU01). */
    public CuentaCreadaResponse crearAdministrador(CrearAdminRequest req) {
        Rol actor = actorEnBd();
        if (!actor.puedeGestionarAdministradores()) {
            throw ApiException.prohibido("Solo el superadministrador puede crear administradores");
        }
        String correo = validarCorreoLibre(req.correo(), null);
        Admin admin = new Admin();
        admin.setNombre(Textos.limpiar(req.nombre()));
        admin.setCorreo(correo);
        admin.setTelefono(Textos.limpiar(req.telefono()));
        admin.setCargo(Textos.limpiar(req.cargo()));
        String temporal = asignarClave(admin, req.password());
        Admin guardado = repositorio.save(admin);
        auditoria.registrar(AccionAuditoria.CREAR, "Usuario", guardado.getId(), "Administrador " + correo);
        return new CuentaCreadaResponse(UsuarioResponse.de(guardado), temporal,
                "Administrador creado. Ya puede iniciar sesion" + (temporal != null ? " con la clave temporal." : "."));
    }

    /** Un administrador (o el superadministrador) registra un donante (RF003 / HU03). */
    public CuentaCreadaResponse crearDonante(CrearDonanteRequest req) {
        String correo = validarCorreoLibre(req.correo(), null);
        String identificacion = validarIdentificacionLibre(req.identificacion(), null);
        Donante d = new Donante();
        d.setNombre(Textos.limpiar(req.nombre()));
        d.setCorreo(correo);
        d.setTelefono(Textos.limpiar(req.telefono()));
        d.setTipoDonante(req.tipoDonante());
        d.setIdentificacion(identificacion);
        d.setDireccion(Textos.limpiar(req.direccion()));
        d.setCiudad(Textos.limpiar(req.ciudad()));
        d.setPersonaContacto(Textos.limpiar(req.personaContacto()));
        String temporal = asignarClave(d, req.password());
        Donante guardado = repositorio.save(d);
        auditoria.registrar(AccionAuditoria.CREAR, "Usuario", guardado.getId(), "Donante " + correo);
        return new CuentaCreadaResponse(UsuarioResponse.de(guardado), temporal,
                "Donante registrado. Ya puede asociarse a nuevas donaciones.");
    }

    // ------------------------------------------------------------------ consultar

    public Pagina<UsuarioResponse> listarAdministradores(int pagina, int tamano) {
        if (!actorEnBd().puedeGestionarAdministradores()) {
            throw ApiException.prohibido("Solo el superadministrador puede ver los administradores");
        }
        var page = repositorio.findByRolIn(EnumSet.of(TipoRol.ADMIN, TipoRol.SUPERADMIN),
                PageRequest.of(pagina, tamano, Sort.by("nombre")));
        return Pagina.de(page, UsuarioResponse::de);
    }

    /** Busqueda de donantes por nombre (parcial) o por identificacion/correo exactos (HU04). */
    public Pagina<UsuarioResponse> buscarDonantes(String texto, int pagina, int tamano) {
        PageRequest pr = PageRequest.of(pagina, tamano, Sort.by("nombre"));
        if (Textos.vacio(texto)) {
            return Pagina.de(repositorio.findByRolIn(EnumSet.of(TipoRol.DONANTE), pr), UsuarioResponse::de);
        }
        String t = texto.trim();
        return Pagina.de(repositorio.buscarDonantes(Textos.escaparRegex(t), t.toLowerCase().contains("@")
                ? Textos.normalizarCorreo(t) : t, pr), UsuarioResponse::de);
    }

    public UsuarioResponse obtener(String id) {
        Rol actor = actorEnBd();
        Rol objetivo = buscar(id);
        if (!actor.getId().equals(objetivo.getId()) && !actor.puedeGestionar(objetivo)) {
            throw ApiException.prohibido("No tienes permiso para ver esta cuenta");
        }
        return UsuarioResponse.de(objetivo);
    }

    // ------------------------------------------------------------------ editar (HU02, HU04)

    public UsuarioResponse actualizar(String id, ActualizarUsuarioRequest req) {
        Rol actor = actorEnBd();
        Rol u = buscar(id);
        if (!actor.puedeGestionar(u)) {
            throw ApiException.prohibido("No tienes permiso para editar esta cuenta");
        }
        if (req.nombre() != null) {
            u.setNombre(Textos.limpiar(req.nombre()));
        }
        if (req.correo() != null) {
            u.setCorreo(validarCorreoLibre(req.correo(), u.getId()));
        }
        if (req.telefono() != null) {
            u.setTelefono(Textos.limpiar(req.telefono()));
        }
        if (u instanceof Admin a && req.cargo() != null) {
            a.setCargo(Textos.limpiar(req.cargo()));
        }
        if (u instanceof Donante d) {
            if (req.tipoDonante() != null) {
                d.setTipoDonante(req.tipoDonante());
            }
            if (req.identificacion() != null) {
                d.setIdentificacion(validarIdentificacionLibre(req.identificacion(), d.getId()));
            }
            if (req.direccion() != null) {
                d.setDireccion(Textos.limpiar(req.direccion()));
            }
            if (req.ciudad() != null) {
                d.setCiudad(Textos.limpiar(req.ciudad()));
            }
            if (req.personaContacto() != null) {
                d.setPersonaContacto(Textos.limpiar(req.personaContacto()));
            }
        }
        Rol guardado = repositorio.save(u);
        auditoria.registrar(AccionAuditoria.ACTUALIZAR, "Usuario", u.getId(), "Datos de " + u.getCorreo());
        return UsuarioResponse.de(guardado);
    }

    /**
     * Cambio de rol (HU02). Solo el superadministrador, solo entre ADMIN y DONANTE.
     * Como cada rol es una CLASE distinta, se crea un objeto de la nueva subclase con los datos comunes
     * y se guarda con el mismo id (el historial de donaciones se conserva).
     */
    public UsuarioResponse cambiarRol(String id, TipoRol nuevoRol) {
        Rol actor = actorEnBd();
        if (!actor.puedeGestionarAdministradores()) {
            throw ApiException.prohibido("Solo el superadministrador puede cambiar roles");
        }
        if (nuevoRol == TipoRol.SUPERADMIN) {
            throw ApiException.solicitudInvalida("No se pueden crear superadministradores desde la API");
        }
        Rol u = buscar(id);
        if (!actor.puedeGestionar(u)) {
            throw ApiException.prohibido("No puedes cambiar el rol de esta cuenta");
        }
        if (u.getRol() == nuevoRol) {
            return UsuarioResponse.de(u);
        }
        Rol nuevo;
        if (nuevoRol == TipoRol.ADMIN) {
            nuevo = new Admin();
        } else {
            Donante d = new Donante();
            d.setTipoDonante(TipoDonante.PERSONA_NATURAL);
            nuevo = d;
        }
        u.copiarDatosComunesA(nuevo);
        Rol guardado = repositorio.save(nuevo);
        authService.invalidarSesiones(id);
        auditoria.registrar(AccionAuditoria.CAMBIAR_ROL, "Usuario", id, u.getRol() + " -> " + nuevoRol);
        return UsuarioResponse.de(guardado);
    }

    public UsuarioResponse cambiarEstado(String id, boolean activo) {
        Rol actor = actorEnBd();
        Rol u = buscar(id);
        if (actor.getId().equals(u.getId())) {
            throw ApiException.solicitudInvalida("No puedes desactivar tu propia cuenta");
        }
        if (!actor.puedeGestionar(u)) {
            throw ApiException.prohibido("No tienes permiso para cambiar el estado de esta cuenta");
        }
        u.setActivo(activo);
        Rol guardado = repositorio.save(u);
        if (!activo) {
            authService.invalidarSesiones(id);
        }
        auditoria.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, "Usuario", id, u.getCorreo());
        return UsuarioResponse.de(guardado);
    }

    /** Genera una clave temporal (se muestra una sola vez) y obliga a cambiarla. */
    public CuentaCreadaResponse restablecerPassword(String id) {
        Rol actor = actorEnBd();
        Rol u = buscar(id);
        if (!actor.puedeGestionar(u)) {
            throw ApiException.prohibido("No tienes permiso para restablecer la clave de esta cuenta");
        }
        String temporal = ClavesTemporales.generar();
        u.setPassword(passwordEncoder.encode(temporal));
        u.setDebeCambiarPassword(true);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        repositorio.save(u);
        authService.invalidarSesiones(id);
        auditoria.registrar(AccionAuditoria.CAMBIAR_PASSWORD, "Usuario", id, "Restablecida por " + actor.getCorreo());
        return new CuentaCreadaResponse(UsuarioResponse.de(u), temporal,
                "Clave restablecida. Entregala al usuario por un medio seguro; debera cambiarla al ingresar.");
    }

    // ------------------------------------------------------------------ perfil propio

    public UsuarioResponse miPerfil() {
        return UsuarioResponse.de(actorEnBd());
    }

    public UsuarioResponse actualizarPerfil(ActualizarPerfilRequest req) {
        Rol u = actorEnBd();
        if (req.nombre() != null) {
            u.setNombre(Textos.limpiar(req.nombre()));
        }
        if (req.telefono() != null) {
            u.setTelefono(Textos.limpiar(req.telefono()));
        }
        if (u instanceof Donante d) {
            if (req.direccion() != null) {
                d.setDireccion(Textos.limpiar(req.direccion()));
            }
            if (req.ciudad() != null) {
                d.setCiudad(Textos.limpiar(req.ciudad()));
            }
            if (req.personaContacto() != null) {
                d.setPersonaContacto(Textos.limpiar(req.personaContacto()));
            }
        }
        Rol guardado = repositorio.save(u);
        auditoria.registrar(AccionAuditoria.ACTUALIZAR, "Usuario", u.getId(), "Perfil propio");
        return UsuarioResponse.de(guardado);
    }

    /** Cambia la clave propia. Cierra todas las sesiones abiertas (hay que volver a iniciar sesion). */
    public void cambiarPassword(CambioPasswordRequest req) {
        Rol u = actorEnBd();
        if (!passwordEncoder.matches(req.actual(), u.getPassword())) {
            throw ApiException.solicitudInvalida("La contrasena actual no es correcta");
        }
        if (Objects.equals(req.actual(), req.nueva())) {
            throw ApiException.solicitudInvalida("La nueva contrasena debe ser diferente a la actual");
        }
        u.setPassword(passwordEncoder.encode(req.nueva()));
        u.setDebeCambiarPassword(false);
        repositorio.save(u);
        authService.invalidarSesiones(u.getId());
        auditoria.registrar(AccionAuditoria.CAMBIAR_PASSWORD, "Usuario", u.getId(), "Cambio de clave propio");
    }

    // ------------------------------------------------------------------ apoyo

    public Rol buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }

    public Donante buscarDonante(String id) {
        Rol u = buscar(id);
        if (u instanceof Donante d) {
            return d;
        }
        throw ApiException.solicitudInvalida("La cuenta indicada no es un donante");
    }

    private Rol actorEnBd() {
        UsuarioAutenticado actual = SecurityUtils.actual();
        return repositorio.findById(actual.id()).orElseThrow(() -> ApiException.noAutorizado("Debes iniciar sesion"));
    }

    private String validarCorreoLibre(String correo, String idPropio) {
        String c = Textos.normalizarCorreo(correo);
        repositorio.findByCorreo(c).ifPresent(otro -> {
            if (!otro.getId().equals(idPropio)) {
                throw ApiException.conflicto("Ya existe una cuenta con ese correo");
            }
        });
        return c;
    }

    private String validarIdentificacionLibre(String identificacion, String idPropio) {
        String i = Textos.limpiar(identificacion);
        if (i == null) {
            return null;
        }
        repositorio.findDonanteByIdentificacion(i).ifPresent(otro -> {
            if (!otro.getId().equals(idPropio)) {
                throw ApiException.conflicto("Ya existe un donante con esa identificacion");
            }
        });
        return i;
    }

    /** Usa la clave enviada o genera una temporal; devuelve la temporal (o null). */
    private String asignarClave(Rol u, String password) {
        if (!Textos.vacio(password)) {
            u.setPassword(passwordEncoder.encode(password));
            u.setDebeCambiarPassword(false);
            return null;
        }
        String temporal = ClavesTemporales.generar();
        u.setPassword(passwordEncoder.encode(temporal));
        u.setDebeCambiarPassword(true);
        return temporal;
    }
}
