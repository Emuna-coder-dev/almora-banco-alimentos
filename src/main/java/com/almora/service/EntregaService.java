package com.almora.service;

import com.almora.dto.comun.Pagina;
import com.almora.dto.entrega.CambioEstadoEntregaRequest;
import com.almora.dto.entrega.EntregaRequest;
import com.almora.dto.entrega.EntregaResponse;
import com.almora.dto.entrega.LoteAsignadoResponse;
import com.almora.dto.entrega.PlanProductoResponse;
import com.almora.dto.entrega.PrevisualizacionEntregaResponse;
import com.almora.dto.entrega.SolicitudItemRequest;
import com.almora.exception.ApiException;
import com.almora.fefo.AsignacionLote;
import com.almora.fefo.FefoAllocator;
import com.almora.fefo.ResultadoFefo;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.donacion.EventoSeguimiento;
import com.almora.model.entrega.Entrega;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.entrega.Faltante;
import com.almora.model.entrega.Fundacion;
import com.almora.model.entrega.ItemEntrega;
import com.almora.model.inventario.Lote;
import com.almora.model.inventario.Producto;
import com.almora.repository.EntregaRepository;
import com.almora.repository.LoteRepository;
import com.almora.security.SecurityUtils;
import com.almora.security.UsuarioAutenticado;
import com.almora.util.Codigos;
import com.almora.util.Textos;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entregas a fundaciones con asignacion FEFO (SCAMPER A1, P2 y R1).
 *
 *  1. previsualizar: el sistema PROPONE de que lotes sale cada producto (los que vencen primero) sin reservar nada.
 *  2. crear: aplica la misma asignacion y RESERVA las unidades de forma atomica. Si otra persona tomo esas
 *     unidades al mismo tiempo, toda la operacion se deshace (transaccion) y se pide intentar de nuevo.
 *  3. cambiarEstado: PROGRAMADA -> EN_RUTA -> ENTREGADA (o CANCELADA, y las unidades vuelven a bodega).
 * Cada paso queda en la ruta de seguimiento de la entrega y de cada donacion involucrada.
 */
@Service
public class EntregaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntregaRepository repositorio;
    private final LoteRepository loteRepository;
    private final MongoTemplate mongoTemplate;
    private final FefoAllocator fefo;
    private final InventarioService inventario;
    private final ProductoService productoService;
    private final FundacionService fundacionService;
    private final DonacionService donacionService;
    private final AuditoriaService auditoria;

    public EntregaService(EntregaRepository repositorio, LoteRepository loteRepository, MongoTemplate mongoTemplate,
                          FefoAllocator fefo, InventarioService inventario, ProductoService productoService,
                          FundacionService fundacionService, DonacionService donacionService,
                          AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.loteRepository = loteRepository;
        this.mongoTemplate = mongoTemplate;
        this.fefo = fefo;
        this.inventario = inventario;
        this.productoService = productoService;
        this.fundacionService = fundacionService;
        this.donacionService = donacionService;
        this.auditoria = auditoria;
    }

    // ================================================================ FEFO

    /** Plan FEFO de un pedido: por producto, la lista de lotes (del que vence primero al ultimo) y lo que falta. */
    private record Plan(Producto producto, ResultadoFefo resultado) {}

    private List<Plan> planificar(List<SolicitudItemRequest> items) {
        // Si el mismo producto viene dos veces, se suman las cantidades
        Map<String, Integer> pedido = new LinkedHashMap<>();
        for (SolicitudItemRequest i : items) {
            pedido.merge(i.productoId(), i.cantidad(), Integer::sum);
        }
        LocalDate hoy = inventario.hoy();
        List<Plan> planes = new ArrayList<>();
        for (Map.Entry<String, Integer> e : pedido.entrySet()) {
            Producto p = productoService.buscar(e.getKey());
            ResultadoFefo r = fefo.asignar(e.getValue(), inventario.candidatosFefo(p.getId()), hoy);
            planes.add(new Plan(p, r));
        }
        return planes;
    }

    public PrevisualizacionEntregaResponse previsualizar(EntregaRequest req) {
        Fundacion f = fundacionActiva(req.fundacionId());
        List<Plan> planes = planificar(req.items());
        Map<String, Lote> lotes = cargarLotes(planes);
        List<PlanProductoResponse> productos = new ArrayList<>();
        boolean completa = true;
        for (Plan plan : planes) {
            List<LoteAsignadoResponse> asignados = new ArrayList<>();
            for (AsignacionLote a : plan.resultado().asignaciones()) {
                Lote l = lotes.get(a.loteId());
                asignados.add(new LoteAsignadoResponse(l.getId(), l.getCodigo(), l.getCodigoDonacion(), a.cantidad(),
                        l.getFechaVencimiento(), inventario.diasParaVencer(l.getFechaVencimiento()),
                        inventario.semaforoDe(l.getFechaVencimiento())));
            }
            completa &= plan.resultado().completo();
            productos.add(new PlanProductoResponse(plan.producto().getId(), plan.producto().getNombre(),
                    plan.resultado().solicitado(), plan.resultado().cubierto(), plan.resultado().noCubierto(), asignados));
        }
        return new PrevisualizacionEntregaResponse(f.getId(), f.getNombre(), completa, productos);
    }

    // ================================================================ crear

    @Transactional
    public EntregaResponse crear(EntregaRequest req) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Fundacion f = fundacionActiva(req.fundacionId());
        List<Plan> planes = planificar(req.items());
        boolean permitirParcial = req.permitirParcial() == null || req.permitirParcial();

        int totalCubierto = planes.stream().mapToInt(p -> p.resultado().cubierto()).sum();
        if (totalCubierto == 0) {
            throw ApiException.conflicto("No hay inventario disponible (sin vencer) para ninguno de los productos solicitados");
        }
        List<Faltante> faltantes = new ArrayList<>();
        for (Plan p : planes) {
            if (!p.resultado().completo()) {
                faltantes.add(new Faltante(p.producto().getId(), p.producto().getNombre(), p.resultado().solicitado(),
                        p.resultado().cubierto(), p.resultado().noCubierto()));
            }
        }
        if (!permitirParcial && !faltantes.isEmpty()) {
            String detalle = faltantes.stream().map(x -> x.getNombreProducto() + " (faltan " + x.getNoCubierto() + ")")
                    .collect(Collectors.joining(", "));
            throw ApiException.conflicto("El inventario no alcanza para todo el pedido: " + detalle);
        }

        // Reserva atomica lote por lote (paso 5 del algoritmo)
        Map<String, Lote> lotes = cargarLotes(planes);
        List<ItemEntrega> items = new ArrayList<>();
        for (Plan p : planes) {
            for (AsignacionLote a : p.resultado().asignaciones()) {
                if (!inventario.reservar(a.loteId(), a.cantidad())) {
                    throw ApiException.conflicto("El inventario cambio mientras se preparaba la entrega. Intenta de nuevo.");
                }
                Lote l = lotes.get(a.loteId());
                ItemEntrega it = new ItemEntrega();
                it.setLoteId(l.getId());
                it.setCodigoLote(l.getCodigo());
                it.setProductoId(l.getProductoId());
                it.setNombreProducto(l.getNombreProducto());
                it.setUnidadMedida(l.getUnidadMedida());
                it.setCantidad(a.cantidad());
                it.setFechaVencimiento(l.getFechaVencimiento());
                it.setDonacionId(l.getDonacionId());
                it.setCodigoDonacion(l.getCodigoDonacion());
                items.add(it);
            }
        }

        Entrega e = new Entrega();
        e.setCodigo(codigoEntregaUnico());
        e.setFundacionId(f.getId());
        e.setFundacionNombre(f.getNombre());
        e.setDireccionDestino(f.getDireccion());
        e.setCiudadDestino(f.getCiudad());
        e.setItems(items);
        e.setFaltantes(faltantes);
        e.setEstado(EstadoEntrega.PROGRAMADA);
        e.setFechaProgramada(req.fechaProgramada() == null ? LocalDate.now(ZONA) : req.fechaProgramada());
        e.setFechaCreacion(Instant.now());
        e.setCreadaPor(actor.etiqueta());
        e.setObservaciones(Textos.limpiar(req.observaciones()));
        e.getSeguimiento().add(EventoSeguimiento.de(EstadoEntrega.PROGRAMADA.name(), "Entrega programada",
                e.totalUnidades() + " unidad(es) para " + f.getNombre() + " el " + e.getFechaProgramada().format(FECHA)
                        + " (lotes elegidos por FEFO: primero los que vencen antes)",
                "Bodega", actor.etiqueta(), e.getCodigo()));
        e = repositorio.save(e);

        Set<String> donaciones = donacionesDe(e);
        donacionService.agregarEvento(donaciones, EventoSeguimiento.de(EstadoDonacion.ASIGNADA.name(),
                "Asignada a una entrega", "Destino: " + destino(e) + ". Fecha programada: "
                        + e.getFechaProgramada().format(FECHA), "Bodega", actor.etiqueta(), e.getCodigo()));
        donaciones.forEach(donacionService::recalcularEstado);

        auditoria.registrar(AccionAuditoria.CREAR, "Entrega", e.getId(),
                e.getCodigo() + " a " + f.getNombre() + " (" + e.totalUnidades() + " und"
                        + (faltantes.isEmpty() ? "" : ", con faltantes") + ")");
        return EntregaResponse.de(e);
    }

    // ================================================================ avanzar

    @Transactional
    public EntregaResponse cambiarEstado(String id, CambioEstadoEntregaRequest req) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Entrega e = buscar(id);
        EstadoEntrega nuevo = req.estado();
        if (!e.getEstado().puedePasarA(nuevo)) {
            throw ApiException.conflicto("No se puede pasar de " + e.getEstado() + " a " + nuevo
                    + ". Permitido: " + e.getEstado().siguientesPermitidos());
        }
        Set<String> donaciones = donacionesDe(e);
        String ubicacion = Textos.limpiar(req.ubicacion());
        String obs = Textos.limpiar(req.observacion());

        switch (nuevo) {
            case EN_RUTA -> {
                String desc = "En camino hacia " + destino(e) + (obs == null ? "" : ". " + obs);
                e.getSeguimiento().add(EventoSeguimiento.de(nuevo.name(), "En ruta", desc,
                        ubicacion == null ? "Salio de bodega" : ubicacion, actor.etiqueta(), e.getCodigo()));
                donacionService.agregarEvento(donaciones, EventoSeguimiento.de(EstadoDonacion.EN_RUTA.name(), "En ruta",
                        desc, ubicacion == null ? "Salio de bodega" : ubicacion, actor.etiqueta(), e.getCodigo()));
            }
            case ENTREGADA -> {
                for (ItemEntrega it : e.getItems()) {
                    inventario.confirmarEntrega(it.getLoteId(), it.getCantidad());
                }
                e.setFechaEntrega(Instant.now());
                e.setReceptor(Textos.limpiar(req.receptor()));
                String desc = "Entregada en " + destino(e)
                        + (e.getReceptor() == null ? "" : ". Recibio: " + e.getReceptor()) + (obs == null ? "" : ". " + obs);
                e.getSeguimiento().add(EventoSeguimiento.de(nuevo.name(), "Entregada", desc,
                        e.getCiudadDestino(), actor.etiqueta(), e.getCodigo()));
                for (String donacionId : donaciones) {
                    int unidades = e.getItems().stream().filter(i -> donacionId.equals(i.getDonacionId()))
                            .mapToInt(ItemEntrega::getCantidad).sum();
                    donacionService.agregarEvento(Set.of(donacionId), EventoSeguimiento.de(EstadoDonacion.ENTREGADA.name(),
                            "Entregada a su destino", unidades + " unidad(es) de tu donacion llegaron a " + destino(e),
                            e.getCiudadDestino(), actor.etiqueta(), e.getCodigo()));
                }
            }
            case CANCELADA -> {
                for (ItemEntrega it : e.getItems()) {
                    inventario.liberar(it.getLoteId(), it.getCantidad());
                }
                String desc = "Entrega cancelada; los productos regresan a bodega" + (obs == null ? "" : ". Motivo: " + obs);
                e.getSeguimiento().add(EventoSeguimiento.de(nuevo.name(), "Cancelada", desc, "Bodega",
                        actor.etiqueta(), e.getCodigo()));
                donacionService.agregarEvento(donaciones, EventoSeguimiento.de("ENTREGA_CANCELADA",
                        "Entrega cancelada", desc, "Bodega", actor.etiqueta(), e.getCodigo()));
            }
            case PROGRAMADA -> throw ApiException.solicitudInvalida("La entrega ya fue programada");
        }
        e.setEstado(nuevo);
        e = repositorio.save(e);
        donaciones.forEach(donacionService::recalcularEstado);
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, "Entrega", id, e.getCodigo() + " -> " + nuevo);
        return EntregaResponse.de(e);
    }

    // ================================================================ consultar

    public Pagina<EntregaResponse> listar(EstadoEntrega estado, String fundacionId, LocalDate desde, LocalDate hasta,
                                          int pagina, int tamano) {
        Criteria c = new Criteria();
        if (estado != null) {
            c.and("estado").is(estado);
        }
        if (!Textos.vacio(fundacionId)) {
            c.and("fundacionId").is(fundacionId);
        }
        if (desde != null || hasta != null) {
            Criteria f = c.and("fechaCreacion");
            if (desde != null) {
                f.gte(desde.atStartOfDay(ZONA).toInstant());
            }
            if (hasta != null) {
                f.lt(hasta.plusDays(1).atStartOfDay(ZONA).toInstant());
            }
        }
        Query q = new Query(c);
        long total = mongoTemplate.count(q, Entrega.class);
        q.with(Sort.by(Sort.Direction.DESC, "fechaCreacion")).skip((long) pagina * tamano).limit(tamano);
        List<EntregaResponse> lista = mongoTemplate.find(q, Entrega.class).stream().map(EntregaResponse::de).toList();
        return new Pagina<>(lista, pagina, tamano, total, (int) Math.ceil((double) total / tamano));
    }

    public EntregaResponse obtener(String id) {
        return EntregaResponse.de(buscar(id));
    }

    public Entrega buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Entrega no encontrada"));
    }

    // ================================================================ apoyo

    private Fundacion fundacionActiva(String id) {
        Fundacion f = fundacionService.buscar(id);
        if (!f.isActiva()) {
            throw ApiException.solicitudInvalida("La fundacion '" + f.getNombre() + "' esta desactivada");
        }
        return f;
    }

    private Map<String, Lote> cargarLotes(List<Plan> planes) {
        List<String> ids = planes.stream().flatMap(p -> p.resultado().asignaciones().stream())
                .map(AsignacionLote::loteId).toList();
        return loteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Lote::getId, Function.identity()));
    }

    private Set<String> donacionesDe(Entrega e) {
        return e.getItems().stream().map(ItemEntrega::getDonacionId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String destino(Entrega e) {
        String ciudad = e.getCiudadDestino() == null ? "" : ", " + e.getCiudadDestino();
        return e.getFundacionNombre() + " (" + e.getDireccionDestino() + ciudad + ")";
    }

    private String codigoEntregaUnico() {
        for (int i = 0; i < 5; i++) {
            String codigo = Codigos.generar("ENT");
            if (!repositorio.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        return Codigos.generar("ENT", 10);
    }
}
