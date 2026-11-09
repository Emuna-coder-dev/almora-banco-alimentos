package com.almora.service;

import com.almora.config.AlmoraProperties;
import com.almora.dto.auth.AuthResponse;
import com.almora.dto.auth.LoginRequest;
import com.almora.dto.auth.RegistroDonanteRequest;
import com.almora.dto.usuario.UsuarioResponse;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.RefreshToken;
import com.almora.model.usuario.Rol;
import com.almora.repository.RefreshTokenRepository;
import com.almora.repository.UsuarioRepository;
import com.almora.security.JwtService;
import com.almora.util.Textos;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Autenticacion con JWT (RNF-S02).
 *
 * Flujo:
 *  1. login: valida correo y contrasena (BCrypt) y entrega:
 *       - un token de ACCESO (JWT, 15 min) que el frontend envia en "Authorization: Bearer ..."
 *       - un token de REFRESCO (7 dias) que viaja solo en una cookie HttpOnly
 *  2. refresh: cambia el token de refresco por uno nuevo (rotacion) y entrega otro token de acceso.
 *     Si alguien reutiliza un token de refresco viejo (posible robo), se cierra toda esa sesion.
 *  3. logout: revoca el token de refresco.
 *
 * Protecciones: bloqueo temporal tras 5 intentos fallidos, mensaje de error identico si el correo
 * no existe o la clave esta mal (no revela que correos estan registrados), y auditoria de cada intento.
 */
@Service
public class AuthService {

    /** Resultado del login/refresh: la respuesta JSON y el token de refresco para ponerlo en la cookie. */
    public record Sesion(AuthResponse respuesta, String refreshToken, long segundosRefresco) {}

    private static final String CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoria;
    private final AlmoraProperties propiedades;
    /** Hash falso para comparar cuando el correo no existe: asi la respuesta tarda lo mismo. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                       MongoTemplate mongoTemplate, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuditoriaService auditoria, AlmoraProperties propiedades) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
        this.hashFicticio = passwordEncoder.encode("clave-ficticia-" + UUID.randomUUID());
    }

    // ------------------------------------------------------------------ registro

    /** Registro publico de donantes (HU01/HU03). Siempre crea un Donante; nadie puede auto-asignarse otro rol. */
    public UsuarioResponse registrarDonante(RegistroDonanteRequest req) {
        String correo = Textos.normalizarCorreo(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw ApiException.conflicto("Ya existe una cuenta con ese correo");
        }
        String identificacion = Textos.limpiar(req.identificacion());
        if (identificacion != null && usuarioRepository.findDonanteByIdentificacion(identificacion).isPresent()) {
            throw ApiException.conflicto("Ya existe un donante con esa identificacion");
        }
        Donante d = new Donante();
        d.setNombre(Textos.limpiar(req.nombre()));
        d.setCorreo(correo);
        d.setPassword(passwordEncoder.encode(req.password()));
        d.setTelefono(Textos.limpiar(req.telefono()));
        d.setTipoDonante(req.tipoDonante());
        d.setIdentificacion(identificacion);
        d.setDireccion(Textos.limpiar(req.direccion()));
        d.setCiudad(Textos.limpiar(req.ciudad()));
        d.setPersonaContacto(Textos.limpiar(req.personaContacto()));
        Donante guardado = usuarioRepository.save(d);
        auditoria.registrarComo(guardado.getId(), guardado.getCorreo(), guardado.getRol().name(),
                AccionAuditoria.CREAR, "Usuario", guardado.getId(), "Registro publico de donante");
        return UsuarioResponse.de(guardado);
    }

    // ------------------------------------------------------------------ login

    public Sesion login(LoginRequest req, String ip, String userAgent) {
        String correo = Textos.normalizarCorreo(req.correo());
        Rol usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        Instant ahora = Instant.now();

        if (usuario == null) {
            passwordEncoder.matches(req.password(), hashFicticio);
            auditoria.registrarComo(null, correo, null, AccionAuditoria.LOGIN_FALLIDO, "Usuario", null, "Correo no registrado");
            throw ApiException.noAutorizado(CREDENCIALES_INVALIDAS);
        }
        if (usuario.estaBloqueado(ahora)) {
            long minutos = Math.max(1, ChronoUnit.MINUTES.between(ahora, usuario.getBloqueadoHasta()) + 1);
            throw new ApiException(HttpStatus.LOCKED,
                    "Cuenta bloqueada temporalmente por varios intentos fallidos. Intenta de nuevo en " + minutos + " minuto(s).");
        }
        if (!passwordEncoder.matches(req.password(), usuario.getPassword())) {
            registrarIntentoFallido(usuario, ahora);
            throw ApiException.noAutorizado(CREDENCIALES_INVALIDAS);
        }
        if (!usuario.isActivo()) {
            auditoria.registrarComo(usuario.getId(), usuario.getCorreo(), usuario.getRol().name(),
                    AccionAuditoria.LOGIN_FALLIDO, "Usuario", usuario.getId(), "Cuenta desactivada");
            throw ApiException.noAutorizado("Tu cuenta esta desactivada. Comunicate con la fundacion.");
        }

        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(usuario.getId())),
                new Update().set("intentosFallidos", 0).unset("bloqueadoHasta").set("ultimoAcceso", ahora),
                Rol.class);
        usuario.setUltimoAcceso(ahora);
        auditoria.registrarComo(usuario.getId(), usuario.getCorreo(), usuario.getRol().name(),
                AccionAuditoria.LOGIN_EXITOSO, "Usuario", usuario.getId(), null);

        return emitirSesion(usuario, UUID.randomUUID().toString(), ip, userAgent);
    }

    private void registrarIntentoFallido(Rol usuario, Instant ahora) {
        int max = propiedades.seguridad().maxIntentos();
        int intentos = usuario.getIntentosFallidos() + 1;
        Update update = new Update();
        if (intentos >= max) {
            Instant hasta = ahora.plus(propiedades.seguridad().minutosBloqueo(), ChronoUnit.MINUTES);
            update.set("intentosFallidos", 0).set("bloqueadoHasta", hasta);
            auditoria.registrarComo(usuario.getId(), usuario.getCorreo(), usuario.getRol().name(),
                    AccionAuditoria.CUENTA_BLOQUEADA, "Usuario", usuario.getId(),
                    "Bloqueada hasta " + hasta + " tras " + max + " intentos fallidos");
        } else {
            update.set("intentosFallidos", intentos);
        }
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(usuario.getId())), update, Rol.class);
        auditoria.registrarComo(usuario.getId(), usuario.getCorreo(), usuario.getRol().name(),
                AccionAuditoria.LOGIN_FALLIDO, "Usuario", usuario.getId(), "Contrasena incorrecta (intento " + intentos + ")");
    }

    // ------------------------------------------------------------------ refresh / logout

    public Sesion refrescar(String tokenRefresco, String ip, String userAgent) {
        if (Textos.vacio(tokenRefresco)) {
            throw ApiException.noAutorizado("No hay una sesion activa");
        }
        RefreshToken guardado = refreshTokenRepository.findByTokenHash(hash(tokenRefresco))
                .orElseThrow(() -> ApiException.noAutorizado("Sesion invalida. Inicia sesion de nuevo."));

        if (guardado.isRevocado()) {
            // Alguien uso un token que ya habia sido reemplazado: posible robo. Se cierra toda la familia.
            revocarFamilia(guardado.getFamilia());
            auditoria.registrarComo(guardado.getUsuarioId(), null, null, AccionAuditoria.REUTILIZACION_TOKEN,
                    "Sesion", guardado.getFamilia(), "Se reutilizo un token de refresco revocado; se cerro la sesion");
            throw ApiException.noAutorizado("Sesion invalida. Inicia sesion de nuevo.");
        }
        Instant ahora = Instant.now();
        if (guardado.getExpiraEn() == null || guardado.getExpiraEn().isBefore(ahora)) {
            throw ApiException.noAutorizado("Tu sesion expiro. Inicia sesion de nuevo.");
        }
        Rol usuario = usuarioRepository.findById(guardado.getUsuarioId())
                .orElseThrow(() -> ApiException.noAutorizado("Sesion invalida. Inicia sesion de nuevo."));
        boolean invalidada = usuario.getSesionesInvalidadasEn() != null
                && guardado.getCreadoEn() != null
                && guardado.getCreadoEn().isBefore(usuario.getSesionesInvalidadasEn());
        if (!usuario.isActivo() || invalidada) {
            revocarFamilia(guardado.getFamilia());
            throw ApiException.noAutorizado("Tu sesion ya no es valida. Inicia sesion de nuevo.");
        }

        // Rotacion: el token usado queda revocado (solo si nadie lo uso al mismo tiempo)
        var resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(guardado.getId()).and("revocado").is(false)),
                Update.update("revocado", true), RefreshToken.class);
        if (resultado.getModifiedCount() == 0) {
            revocarFamilia(guardado.getFamilia());
            throw ApiException.noAutorizado("Sesion invalida. Inicia sesion de nuevo.");
        }
        return emitirSesion(usuario, guardado.getFamilia(), ip, userAgent);
    }

    public void logout(String tokenRefresco) {
        if (Textos.vacio(tokenRefresco)) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(tokenRefresco)).ifPresent(t -> {
            revocarFamilia(t.getFamilia());
            auditoria.registrarComo(t.getUsuarioId(), null, null, AccionAuditoria.LOGOUT, "Sesion", t.getFamilia(), null);
        });
    }

    /** Cierra todas las sesiones de un usuario (cambio de clave, de rol o desactivacion). */
    public void invalidarSesiones(String usuarioId) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(usuarioId)),
                Update.update("sesionesInvalidadasEn", Instant.now()), Rol.class);
        mongoTemplate.updateMulti(Query.query(Criteria.where("usuarioId").is(usuarioId).and("revocado").is(false)),
                Update.update("revocado", true), RefreshToken.class);
    }

    public long segundosRefresco() {
        return propiedades.jwt().diasRefresco() * 24 * 3600;
    }

    // ------------------------------------------------------------------ apoyo

    private Sesion emitirSesion(Rol usuario, String familia, String ip, String userAgent) {
        String acceso = jwtService.generarTokenAcceso(usuario);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refresco = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hash(refresco));
        rt.setUsuarioId(usuario.getId());
        rt.setFamilia(familia);
        rt.setCreadoEn(Instant.now());
        rt.setExpiraEn(Instant.now().plusSeconds(segundosRefresco()));
        rt.setRevocado(false);
        rt.setIp(ip);
        rt.setUserAgent(userAgent == null ? null : userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent);
        refreshTokenRepository.save(rt);

        AuthResponse respuesta = AuthResponse.bearer(acceso, jwtService.segundosDeVidaAcceso(), UsuarioResponse.de(usuario));
        return new Sesion(respuesta, refresco, segundosRefresco());
    }

    private void revocarFamilia(String familia) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("familia").is(familia).and("revocado").is(false)),
                Update.update("revocado", true), RefreshToken.class);
    }

    /** SHA-256: en la base de datos nunca se guarda el token de refresco real. */
    static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
