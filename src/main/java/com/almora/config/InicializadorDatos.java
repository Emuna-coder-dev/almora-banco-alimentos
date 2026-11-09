package com.almora.config;

import com.almora.dto.comun.Validaciones;
import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.Producto;
import com.almora.model.inventario.UnidadMedida;
import com.almora.model.usuario.SuperAdmin;
import com.almora.model.usuario.TipoRol;
import com.almora.repository.ProductoRepository;
import com.almora.repository.UsuarioRepository;
import com.almora.util.Textos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales al arrancar:
 *  1. Si no existe ningun superadministrador, lo crea con SUPERADMIN_EMAIL y SUPERADMIN_PASSWORD (.env).
 *     Es la UNICA forma de crear un superadministrador: no hay endpoint para eso.
 *  2. Si DATOS_DEMO=true y el catalogo esta vacio, crea productos de ejemplo.
 */
@Component
@Order(1)
public class InicializadorDatos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDatos.class);

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlmoraProperties propiedades;

    public InicializadorDatos(UsuarioRepository usuarioRepository, ProductoRepository productoRepository,
                              PasswordEncoder passwordEncoder, AlmoraProperties propiedades) {
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearSuperAdmin();
        if (propiedades.datosDemo()) {
            crearCatalogoDemo();
        }
    }

    private void crearSuperAdmin() {
        if (usuarioRepository.existsByRol(TipoRol.SUPERADMIN)) {
            return;
        }
        var cfg = propiedades.superadmin();
        String correo = cfg == null ? null : Textos.normalizarCorreo(cfg.correo());
        String clave = cfg == null ? null : cfg.password();
        if (Textos.vacio(correo) || Textos.vacio(clave)) {
            log.warn("No hay superadministrador. Define SUPERADMIN_EMAIL y SUPERADMIN_PASSWORD en el archivo .env y reinicia.");
            return;
        }
        if (!clave.matches(Validaciones.CLAVE)) {
            log.error("SUPERADMIN_PASSWORD no cumple la politica: {}. No se creo el superadministrador.", Validaciones.CLAVE_MENSAJE);
            return;
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            log.error("Ya existe una cuenta con {} que no es superadministrador. Usa otro SUPERADMIN_EMAIL.", correo);
            return;
        }
        SuperAdmin s = new SuperAdmin();
        s.setNombre(Textos.vacio(cfg.nombre()) ? "Super Administrador" : cfg.nombre().trim());
        s.setCorreo(correo);
        s.setPassword(passwordEncoder.encode(clave));
        s.setCargo("Superadministrador");
        s.setDebeCambiarPassword(true);
        usuarioRepository.save(s);
        log.info("Superadministrador creado: {}. Cambia la clave en el primer ingreso.", correo);
    }

    private void crearCatalogoDemo() {
        if (productoRepository.count() > 0) {
            return;
        }
        Object[][] catalogo = {
                {"Arroz blanco", CategoriaProducto.GRANOS_Y_CEREALES, false, UnidadMedida.KILOGRAMO},
                {"Frijol rojo", CategoriaProducto.GRANOS_Y_CEREALES, false, UnidadMedida.KILOGRAMO},
                {"Lentejas", CategoriaProducto.GRANOS_Y_CEREALES, false, UnidadMedida.KILOGRAMO},
                {"Pasta", CategoriaProducto.GRANOS_Y_CEREALES, false, UnidadMedida.PAQUETE},
                {"Leche entera UHT", CategoriaProducto.LACTEOS, true, UnidadMedida.LITRO},
                {"Queso campesino", CategoriaProducto.LACTEOS, true, UnidadMedida.LIBRA},
                {"Huevos", CategoriaProducto.CARNES_Y_PROTEINAS, true, UnidadMedida.UNIDAD},
                {"Atun en lata", CategoriaProducto.ENLATADOS_Y_CONSERVAS, true, UnidadMedida.UNIDAD},
                {"Pan tajado", CategoriaProducto.PANADERIA, true, UnidadMedida.PAQUETE},
                {"Platano", CategoriaProducto.FRUTAS_Y_VERDURAS, true, UnidadMedida.KILOGRAMO},
                {"Aceite vegetal", CategoriaProducto.ACEITES_Y_CONDIMENTOS, false, UnidadMedida.LITRO},
                {"Panela", CategoriaProducto.OTRO, false, UnidadMedida.UNIDAD},
                {"Agua en botella", CategoriaProducto.BEBIDAS, false, UnidadMedida.LITRO},
                {"Jabon de bano", CategoriaProducto.ASEO_PERSONAL, false, UnidadMedida.UNIDAD}
        };
        for (Object[] fila : catalogo) {
            Producto p = new Producto();
            p.setNombre((String) fila[0]);
            p.setNombreNormalizado(Producto.normalizar((String) fila[0]));
            p.setCategoria((CategoriaProducto) fila[1]);
            p.setPerecedero((Boolean) fila[2]);
            p.setUnidadMedida((UnidadMedida) fila[3]);
            p.setActivo(true);
            productoRepository.save(p);
        }
        log.info("Catalogo de ejemplo creado con {} productos", catalogo.length);
    }
}
