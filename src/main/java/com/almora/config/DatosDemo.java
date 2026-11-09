package com.almora.config;

import com.almora.dto.donacion.DonacionRequest;
import com.almora.dto.donacion.ItemDonacionRequest;
import com.almora.dto.entrega.CambioEstadoEntregaRequest;
import com.almora.dto.entrega.EntregaRequest;
import com.almora.dto.entrega.SolicitudItemRequest;
import com.almora.dto.fundacion.FundacionRequest;
import com.almora.dto.fundacion.FundacionResponse;
import com.almora.dto.usuario.CrearDonanteRequest;
import com.almora.dto.usuario.CuentaCreadaResponse;
import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.inventario.Producto;
import com.almora.model.usuario.Rol;
import com.almora.model.usuario.TipoDonante;
import com.almora.model.usuario.TipoRol;
import com.almora.repository.DonacionRepository;
import com.almora.repository.FundacionRepository;
import com.almora.repository.ProductoRepository;
import com.almora.repository.UsuarioRepository;
import com.almora.security.UsuarioAutenticado;
import com.almora.service.DonacionService;
import com.almora.service.EntregaService;
import com.almora.service.FundacionService;
import com.almora.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Datos de demostracion (solo si DATOS_DEMO=true y la base aun no tiene donaciones):
 * 3 donantes, 4 fundaciones, 4 donaciones con lotes que vencen en fechas distintas y 4 entregas
 * en diferentes estados. Asi el tablero se ve con informacion real desde el primer arranque.
 *
 * Todo se crea con los MISMOS servicios que usa la API (validaciones, FEFO, auditoria y seguimiento),
 * actuando como el superadministrador. Las fechas son relativas a hoy, nunca quedan vencidas.
 *
 * Cuentas de donante de prueba: clave Donante2026 (ver la guia).
 *
 * Es reanudable: si un arranque anterior alcanzo a crear donantes o fundaciones, los reutiliza.
 * Cada paso se reintenta si MongoDB responde WriteConflict (pasa cuando el correo de confirmacion,
 * que corre en segundo plano, toca la misma donacion al mismo tiempo).
 */
@Component
@Order(2)
public class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);
    public static final String CLAVE_DONANTES = "Donante2026";

    private final AlmoraProperties propiedades;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final DonacionRepository donacionRepository;
    private final FundacionRepository fundacionRepository;
    private final UsuarioService usuarioService;
    private final FundacionService fundacionService;
    private final DonacionService donacionService;
    private final EntregaService entregaService;

    public DatosDemo(AlmoraProperties propiedades, UsuarioRepository usuarioRepository,
                     ProductoRepository productoRepository, DonacionRepository donacionRepository,
                     FundacionRepository fundacionRepository, UsuarioService usuarioService, FundacionService fundacionService,
                     DonacionService donacionService, EntregaService entregaService) {
        this.propiedades = propiedades;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.donacionRepository = donacionRepository;
        this.fundacionRepository = fundacionRepository;
        this.usuarioService = usuarioService;
        this.fundacionService = fundacionService;
        this.donacionService = donacionService;
        this.entregaService = entregaService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!propiedades.datosDemo() || donacionRepository.count() > 0) {
            return;
        }
        Rol superAdmin = usuarioRepository.findByRolIn(List.of(TipoRol.SUPERADMIN),
                org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        if (superAdmin == null) {
            return;
        }
        // Se actua como el superadministrador para que auditoria y seguimiento queden con su nombre
        UsuarioAutenticado actor = new UsuarioAutenticado(superAdmin.getId(), superAdmin.getCorreo(),
                superAdmin.getNombre(), TipoRol.SUPERADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority(TipoRol.SUPERADMIN.autoridad()))));
        try {
            crear();
            log.info("Datos de demostracion creados. Donantes de prueba: donaciones@elahorro.co, "
                    + "contacto@panaderiaelsol.co y maria.gomez@correo.co (clave {})", CLAVE_DONANTES);
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear todos los datos de demostracion: {}", e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void crear() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        String ahorro = donante("Supermercado El Ahorro", "donaciones@elahorro.co", TipoDonante.SUPERMERCADO,
                "900123456-7", "Cra 3 #45-10, Bocagrande", "3001234567");
        String panaderia = donante("Panadería El Sol", "contacto@panaderiaelsol.co", TipoDonante.EMPRESA,
                "901555222-1", "Calle 30 #20-15, Pie de la Popa", "3157654321");
        String maria = donante("María Gómez", "maria.gomez@correo.co", TipoDonante.PERSONA_NATURAL,
                "1047382910", "Barrio Manga, Calle 25 #18-40", "3209876543");

        String vidaNueva = fundacion("Fundación Vida Nueva", "900111222-3", "Carlos Ruiz", "3011112233", "Barrio Olaya Herrera, Sector 11");
        String sanJose = fundacion("Hogar Infantil San José", "900333444-5", "Hna. Lucía Pérez", "3024445566", "Barrio San José de los Campanos");
        String manos = fundacion("Fundación Manos Unidas", "900555666-7", "Andrés Molina", "3037778899", "Barrio Nelson Mandela");
        String paz = fundacion("Asociación Paz y Bien", "900777888-9", "Rosa Martínez", "3049990011", "Barrio El Pozón");

        donacion(ahorro, "Bodega principal - Estante A", List.of(
                item("Arroz blanco", 50, hoy.plusDays(240)),
                item("Leche entera UHT", 30, hoy.plusDays(4)),
                item("Leche entera UHT", 40, hoy.plusDays(45)),
                item("Atun en lata", 15, hoy.plusDays(300)),
                item("Aceite vegetal", 20, null)), true);
        donacion(panaderia, "Bodega principal - Zona fria", List.of(
                item("Pan tajado", 10, hoy.plusDays(3)),
                item("Huevos", 90, hoy.plusDays(12)),
                item("Queso campesino", 12, hoy.plusDays(6))), true);
        donacion(maria, "Bodega principal - Estante B", List.of(
                item("Frijol rojo", 40, hoy.plusDays(200)),
                item("Lentejas", 25, hoy.plusDays(180)),
                item("Platano", 20, hoy.plusDays(5)),
                item("Panela", 30, null)), false);
        donacion(ahorro, "Bodega principal - Estante C", List.of(
                item("Pasta", 35, hoy.plusDays(150)),
                item("Agua en botella", 48, null),
                item("Jabon de bano", 24, null)), false);

        // Entregas con FEFO: salen primero la leche y el pan que vencen antes
        String e1 = entrega(vidaNueva, List.of(sol("Leche entera UHT", 25), sol("Arroz blanco", 20), sol("Pan tajado", 6)));
        avanzar(e1, EstadoEntrega.EN_RUTA, null);
        avanzar(e1, EstadoEntrega.ENTREGADA, "Carlos Ruiz");
        String e2 = entrega(sanJose, List.of(sol("Huevos", 60), sol("Leche entera UHT", 15)));
        avanzar(e2, EstadoEntrega.EN_RUTA, null);
        avanzar(e2, EstadoEntrega.ENTREGADA, "Hna. Lucía Pérez");
        String e3 = entrega(manos, List.of(sol("Frijol rojo", 20), sol("Lentejas", 10), sol("Atun en lata", 10), sol("Platano", 10)));
        avanzar(e3, EstadoEntrega.EN_RUTA, null);
        avanzar(e3, EstadoEntrega.ENTREGADA, "Andrés Molina");
        String e4 = entrega(paz, List.of(sol("Queso campesino", 8), sol("Aceite vegetal", 10)));
        avanzar(e4, EstadoEntrega.EN_RUTA, null);
    }

    // ------------------------------------------------------------------ apoyo

    private String donante(String nombre, String correo, TipoDonante tipo, String identificacion, String direccion, String telefono) {
        Rol existente = usuarioRepository.findByCorreo(correo).orElse(null);
        if (existente != null) {
            return existente.getId();
        }
        CuentaCreadaResponse r = reintentar(() -> usuarioService.crearDonante(new CrearDonanteRequest(nombre, correo,
                CLAVE_DONANTES, telefono, tipo, identificacion, direccion, "Cartagena", null)));
        return r.usuario().id();
    }

    private String fundacion(String nombre, String nit, String contacto, String telefono, String direccion) {
        var existente = fundacionRepository.findByNit(nit);
        if (existente.isPresent()) {
            return existente.get().getId();
        }
        FundacionResponse f = reintentar(() -> fundacionService.crear(
                new FundacionRequest(nombre, nit, contacto, telefono, null, direccion, "Cartagena")));
        return f.id();
    }

    private record ItemDemo(String producto, int cantidad, LocalDate vence) {}

    private ItemDemo item(String producto, int cantidad, LocalDate vence) {
        return new ItemDemo(producto, cantidad, vence);
    }

    private void donacion(String donanteId, String ubicacion, List<ItemDemo> items, boolean almacenar) {
        List<ItemDonacionRequest> lista = new ArrayList<>();
        for (ItemDemo i : items) {
            productoRepository.findByNombreNormalizado(Producto.normalizar(i.producto())).ifPresent(p ->
                    lista.add(new ItemDonacionRequest(p.getId(), i.cantidad(), i.vence())));
        }
        if (lista.isEmpty()) {
            return;
        }
        var d = reintentar(() -> donacionService.registrar(new DonacionRequest(donanteId, lista, null, ubicacion)));
        if (almacenar) {
            reintentar(() -> donacionService.cambiarEstado(d.id(), EstadoDonacion.ALMACENADA, ubicacion, null));
        }
    }

    private SolicitudItemRequest sol(String producto, int cantidad) {
        Producto p = productoRepository.findByNombreNormalizado(Producto.normalizar(producto)).orElse(null);
        return p == null ? null : new SolicitudItemRequest(p.getId(), cantidad);
    }

    private String entrega(String fundacionId, List<SolicitudItemRequest> items) {
        List<SolicitudItemRequest> validos = items.stream().filter(java.util.Objects::nonNull).toList();
        if (validos.isEmpty()) {
            return null;
        }
        return reintentar(() -> entregaService.crear(new EntregaRequest(fundacionId, validos, null, null, true))).id();
    }

    private void avanzar(String entregaId, EstadoEntrega estado, String receptor) {
        if (entregaId != null) {
            reintentar(() -> entregaService.cambiarEstado(entregaId, new CambioEstadoEntregaRequest(estado, receptor,
                    estado == EstadoEntrega.EN_RUTA ? "Salio de bodega" : null, null)));
        }
    }

    /** Repite la operacion si MongoDB la rechazo por un conflicto temporal de escritura (hasta 6 intentos). */
    private <T> T reintentar(Supplier<T> operacion) {
        for (int intento = 1; ; intento++) {
            try {
                return operacion.get();
            } catch (RuntimeException e) {
                if (intento >= 6 || !esConflictoTemporal(e)) {
                    throw e;
                }
                try {
                    Thread.sleep(400L * intento);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private static boolean esConflictoTemporal(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof com.mongodb.MongoException me
                    && (me.hasErrorLabel(com.mongodb.MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                        || me.getCode() == 112)) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && (m.contains("WriteConflict") || m.contains("TransientTransactionError"))) {
                return true;
            }
        }
        return false;
    }
}