package com.almora.service;

import com.almora.config.AlmoraProperties;
import com.almora.dto.comun.Pagina;
import com.almora.dto.inventario.AlertaVencimientoResponse;
import com.almora.dto.inventario.InventarioProductoResponse;
import com.almora.dto.inventario.LoteResponse;
import com.almora.dto.inventario.ResumenSemaforoResponse;
import com.almora.exception.ApiException;
import com.almora.fefo.CalculadoraSemaforo;
import com.almora.fefo.FefoAllocator;
import com.almora.fefo.LoteCandidato;
import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.ItemDonacion;
import com.almora.model.inventario.CategoriaProducto;
import com.almora.model.inventario.EstadoLote;
import com.almora.model.inventario.Lote;
import com.almora.model.inventario.Semaforo;
import com.almora.repository.LoteRepository;
import com.almora.util.Codigos;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventario por LOTES (SCAMPER S2). Cada lote tiene su propia cantidad y fecha de vencimiento.
 *
 * Todas las cantidades se modifican con operaciones ATOMICAS de MongoDB ($inc con condicion),
 * por eso dos entregas al mismo tiempo nunca dejan el stock en negativo ni "pierden" sumas (SCAMPER M2).
 *
 *  - stock disponible de un producto = suma de cantidadDisponible de sus lotes DISPONIBLE (HU09)
 *  - semaforo por lote: VERDE / AMARILLO / ROJO / VENCIDO (SCAMPER A1)
 *  - alertas: lotes que vencen en los proximos N dias, ordenados por fecha (HU10)
 */
@Service
public class InventarioService {

    /** Estados de lote que todavia tienen unidades "vivas" en bodega o en camino. */
    private static final EnumSet<EstadoLote> CON_STOCK = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.AGOTADO);

    private final LoteRepository loteRepository;
    private final MongoTemplate mongoTemplate;
    private final CalculadoraSemaforo semaforo;
    private final AlmoraProperties propiedades;

    public InventarioService(LoteRepository loteRepository, MongoTemplate mongoTemplate,
                             CalculadoraSemaforo semaforo, AlmoraProperties propiedades) {
        this.loteRepository = loteRepository;
        this.mongoTemplate = mongoTemplate;
        this.semaforo = semaforo;
        this.propiedades = propiedades;
    }

    public LocalDate hoy() {
        return LocalDate.now(propiedades.zona());
    }

    // ================================================================ entradas

    /** Crea el lote de un item cuando la donacion se recibe (HU05: "se suma automaticamente al inventario"). */
    public Lote crearLote(Donacion donacion, ItemDonacion item, String ubicacion) {
        Lote l = new Lote();
        l.setCodigo(codigoLoteUnico());
        l.setProductoId(item.getProductoId());
        l.setNombreProducto(item.getNombreProducto());
        l.setCategoria(item.getCategoria());
        l.setPerecedero(item.isPerecedero());
        l.setUnidadMedida(item.getUnidadMedida());
        l.setDonacionId(donacion.getId());
        l.setCodigoDonacion(donacion.getCodigo());
        l.setDonanteId(donacion.getDonanteId());
        l.setCantidadInicial(item.getCantidad());
        l.setCantidadDisponible(item.getCantidad());
        l.setFechaVencimiento(item.getFechaVencimiento());
        l.setFechaIngreso(Instant.now());
        l.setUbicacionAlmacen(ubicacion);
        l.setEstado(EstadoLote.DISPONIBLE);
        return loteRepository.save(l);
    }

    public void actualizarUbicacion(String donacionId, String ubicacion) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("donacionId").is(donacionId)),
                Update.update("ubicacionAlmacen", ubicacion), Lote.class);
    }

    // ================================================================ movimientos atomicos

    /** Reserva n unidades del lote para una entrega. false si el lote ya no tiene esas unidades. */
    public boolean reservar(String loteId, int n) {
        var r = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(loteId)
                        .and("estado").is(EstadoLote.DISPONIBLE)
                        .and("cantidadDisponible").gte(n)),
                new Update().inc("cantidadDisponible", -n).inc("cantidadAsignada", n),
                Lote.class);
        if (r.getModifiedCount() == 0) {
            return false;
        }
        // Si quedo en cero pasa a AGOTADO
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(loteId).and("estado").is(EstadoLote.DISPONIBLE)
                        .and("cantidadDisponible").is(0)),
                Update.update("estado", EstadoLote.AGOTADO), Lote.class);
        return true;
    }

    /** La entrega llego: las unidades reservadas pasan a entregadas. */
    public void confirmarEntrega(String loteId, int n) {
        var r = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(loteId).and("cantidadAsignada").gte(n)),
                new Update().inc("cantidadAsignada", -n).inc("cantidadEntregada", n),
                Lote.class);
        if (r.getModifiedCount() == 0) {
            throw ApiException.conflicto("El lote " + loteId + " no tiene esas unidades reservadas");
        }
    }

    /**
     * La entrega se cancelo: las unidades vuelven a bodega.
     * Si mientras tanto el lote vencio, esas unidades se cuentan como vencidas.
     */
    public void liberar(String loteId, int n) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> ApiException.noEncontrado("Lote no encontrado: " + loteId));
        boolean vencido = lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(hoy());
        Update u = new Update().inc("cantidadAsignada", -n);
        if (vencido) {
            u.inc("cantidadVencida", n);
            if (lote.getCantidadDisponible() == 0) {
                u.set("estado", EstadoLote.VENCIDO);
            }
        } else {
            u.inc("cantidadDisponible", n);
            if (lote.getEstado() == EstadoLote.AGOTADO) {
                u.set("estado", EstadoLote.DISPONIBLE);
            }
        }
        var r = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(loteId).and("cantidadAsignada").gte(n)), u, Lote.class);
        if (r.getModifiedCount() == 0) {
            throw ApiException.conflicto("El lote " + lote.getCodigo() + " no tiene esas unidades reservadas");
        }
    }

    /**
     * HU10: "Los productos vencidos se marcan automaticamente con el estado vencido".
     * Las unidades disponibles de lotes con fecha pasada se mueven a "vencidas".
     * Devuelve los ids de las donaciones afectadas (para actualizar su seguimiento).
     */
    public List<Lote> marcarVencidos() {
        LocalDate hoy = hoy();
        List<Lote> candidatos = mongoTemplate.find(Query.query(
                Criteria.where("estado").is(EstadoLote.DISPONIBLE).and("fechaVencimiento").lt(hoy)), Lote.class);
        List<Lote> afectados = new ArrayList<>();
        for (Lote l : candidatos) {
            int disp = l.getCantidadDisponible();
            var r = mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(l.getId()).and("estado").is(EstadoLote.DISPONIBLE)
                            .and("cantidadDisponible").is(disp)),
                    new Update().set("cantidadDisponible", 0).inc("cantidadVencida", disp).set("estado", EstadoLote.VENCIDO),
                    Lote.class);
            if (r.getModifiedCount() == 1) {
                l.setCantidadVencida(l.getCantidadVencida() + disp);
                l.setCantidadDisponible(0);
                l.setEstado(EstadoLote.VENCIDO);
                afectados.add(l);
            }
        }
        return afectados;
    }

    /** Anulacion de una donacion (HU07, SCAMPER R2): sus lotes salen del inventario una sola vez. */
    public void anularLotesDeDonacion(String donacionId) {
        for (Lote l : loteRepository.findByDonacionId(donacionId)) {
            if (l.getEstado() == EstadoLote.ANULADO) {
                continue;
            }
            int disp = l.getCantidadDisponible();
            var r = mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(l.getId())
                            .and("cantidadAsignada").is(0).and("cantidadEntregada").is(0)
                            .and("cantidadDisponible").is(disp)),
                    new Update().set("cantidadDisponible", 0).inc("cantidadAnulada", disp).set("estado", EstadoLote.ANULADO),
                    Lote.class);
            if (r.getModifiedCount() == 0) {
                throw ApiException.conflicto("El lote " + l.getCodigo()
                        + " ya tiene unidades asignadas o entregadas; la donacion no se puede anular");
            }
        }
    }

    // ================================================================ consultas

    /** Lotes disponibles de un producto convertidos al formato que necesita el algoritmo FEFO. */
    public List<LoteCandidato> candidatosFefo(String productoId) {
        return loteRepository.findByProductoIdAndEstado(productoId, EstadoLote.DISPONIBLE).stream()
                .map(l -> new LoteCandidato(l.getId(), l.getFechaVencimiento(), l.getCantidadDisponible(), l.getFechaIngreso()))
                .toList();
    }

    /** Existencias por producto (HU09), con filtros por tipo y fecha de vencimiento. */
    public List<InventarioProductoResponse> resumenPorProducto(CategoriaProducto categoria, Boolean perecedero,
                                                               LocalDate venceAntesDe, Semaforo filtroSemaforo) {
        LocalDate hoy = hoy();
        Criteria c = Criteria.where("estado").in(CON_STOCK);
        if (categoria != null) {
            c.and("categoria").is(categoria);
        }
        if (perecedero != null) {
            c.and("perecedero").is(perecedero);
        }
        List<Lote> lotes = mongoTemplate.find(Query.query(c), Lote.class);

        Map<String, List<Lote>> porProducto = new LinkedHashMap<>();
        for (Lote l : lotes) {
            porProducto.computeIfAbsent(l.getProductoId(), k -> new ArrayList<>()).add(l);
        }

        List<InventarioProductoResponse> resultado = new ArrayList<>();
        for (List<Lote> grupo : porProducto.values()) {
            Lote muestra = grupo.get(0);
            long disponible = 0, asignado = 0;
            int lotesDisp = 0;
            LocalDate proximo = null;
            for (Lote l : grupo) {
                disponible += l.getCantidadDisponible();
                asignado += l.getCantidadAsignada();
                if (l.getCantidadDisponible() > 0) {
                    lotesDisp++;
                    if (l.getFechaVencimiento() != null && (proximo == null || l.getFechaVencimiento().isBefore(proximo))) {
                        proximo = l.getFechaVencimiento();
                    }
                }
            }
            Semaforo s = disponible == 0 ? null : semaforo.calcular(proximo, hoy);
            Long dias = proximo == null ? null : CalculadoraSemaforo.diasParaVencer(proximo, hoy);
            String estadoStock = disponible == 0 ? "AGOTADO"
                    : (s == Semaforo.ROJO || s == Semaforo.VENCIDO) ? "POR_VENCER" : "DISPONIBLE";

            if (venceAntesDe != null && (proximo == null || proximo.isAfter(venceAntesDe))) {
                continue;
            }
            if (filtroSemaforo != null && filtroSemaforo != s) {
                continue;
            }
            resultado.add(new InventarioProductoResponse(muestra.getProductoId(), muestra.getNombreProducto(),
                    muestra.getCategoria(), muestra.isPerecedero(), muestra.getUnidadMedida(), disponible, asignado,
                    lotesDisp, proximo, dias, s, estadoStock));
        }
        // Primero lo que vence antes (los sin fecha al final), luego por nombre
        resultado.sort(Comparator.comparing(InventarioProductoResponse::proximoVencimiento,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InventarioProductoResponse::nombre, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    /** Lotes con filtros, en ORDEN FEFO (el que vence primero arriba). */
    public Pagina<LoteResponse> lotes(String productoId, EstadoLote estado, CategoriaProducto categoria,
                                      LocalDate venceAntesDe, Semaforo filtroSemaforo, String donacionId,
                                      int pagina, int tamano) {
        Criteria c = new Criteria();
        if (productoId != null) {
            c.and("productoId").is(productoId);
        }
        if (estado != null) {
            c.and("estado").is(estado);
        } else {
            c.and("estado").in(CON_STOCK);
        }
        if (categoria != null) {
            c.and("categoria").is(categoria);
        }
        if (venceAntesDe != null) {
            c.and("fechaVencimiento").lte(venceAntesDe);
        }
        if (donacionId != null) {
            c.and("donacionId").is(donacionId);
        }
        List<Lote> lotes = new ArrayList<>(mongoTemplate.find(Query.query(c), Lote.class));
        lotes.sort(ordenFefoLotes());
        List<LoteResponse> respuesta = aRespuestas(lotes);
        if (filtroSemaforo != null) {
            respuesta = respuesta.stream().filter(l -> l.semaforo() == filtroSemaforo).toList();
        }
        return Pagina.de(respuesta, pagina, tamano);
    }

    /** HU10: lotes con unidades que vencen dentro de los proximos N dias (incluye hoy), del mas proximo al mas lejano. */
    public AlertaVencimientoResponse alertas(Integer dias) {
        int n = dias == null ? propiedades.fefo().diasAlerta() : Math.max(0, Math.min(dias, 365));
        LocalDate hoy = hoy();
        List<Lote> lotes = new ArrayList<>(mongoTemplate.find(Query.query(
                Criteria.where("estado").is(EstadoLote.DISPONIBLE)
                        .and("cantidadDisponible").gt(0)
                        .and("fechaVencimiento").gte(hoy).lte(hoy.plusDays(n))), Lote.class));
        lotes.sort(ordenFefoLotes());
        List<LoteResponse> respuesta = aRespuestas(lotes);
        long unidades = lotes.stream().mapToLong(Lote::getCantidadDisponible).sum();
        return new AlertaVencimientoResponse(n, respuesta.size(), unidades, respuesta);
    }

    public ResumenSemaforoResponse resumenSemaforo() {
        LocalDate hoy = hoy();
        long verde = 0, amarillo = 0, rojo = 0, vencido = 0, sinFecha = 0;
        List<Lote> lotes = mongoTemplate.find(Query.query(
                Criteria.where("estado").is(EstadoLote.DISPONIBLE).and("cantidadDisponible").gt(0)), Lote.class);
        for (Lote l : lotes) {
            switch (semaforo.calcular(l.getFechaVencimiento(), hoy)) {
                case VERDE -> verde++;
                case AMARILLO -> amarillo++;
                case ROJO -> rojo++;
                case VENCIDO -> vencido++;
                case SIN_VENCIMIENTO -> sinFecha++;
            }
        }
        return new ResumenSemaforoResponse(verde, amarillo, rojo, vencido, sinFecha,
                semaforo.getDiasAmarillo(), semaforo.getDiasRojo());
    }

    public Semaforo semaforoDe(LocalDate fechaVencimiento) {
        return semaforo.calcular(fechaVencimiento, hoy());
    }

    public Long diasParaVencer(LocalDate fechaVencimiento) {
        return fechaVencimiento == null ? null : CalculadoraSemaforo.diasParaVencer(fechaVencimiento, hoy());
    }

    /** Convierte lotes (ya en orden FEFO) a respuesta, numerando la posicion FEFO dentro de cada producto. */
    public List<LoteResponse> aRespuestas(List<Lote> lotesOrdenados) {
        LocalDate hoy = hoy();
        Map<String, Integer> posicion = new HashMap<>();
        List<LoteResponse> lista = new ArrayList<>(lotesOrdenados.size());
        for (Lote l : lotesOrdenados) {
            Integer orden = null;
            boolean saleEnFefo = l.getEstado() == EstadoLote.DISPONIBLE && l.getCantidadDisponible() > 0
                    && (l.getFechaVencimiento() == null || !l.getFechaVencimiento().isBefore(hoy));
            if (saleEnFefo) {
                orden = posicion.merge(l.getProductoId(), 1, Integer::sum);
            }
            lista.add(new LoteResponse(l.getId(), l.getCodigo(), l.getProductoId(), l.getNombreProducto(),
                    l.getCategoria(), l.isPerecedero(), l.getUnidadMedida(), l.getDonacionId(), l.getCodigoDonacion(),
                    l.getCantidadInicial(), l.getCantidadDisponible(), l.getCantidadAsignada(), l.getCantidadEntregada(),
                    l.getCantidadVencida(), l.getCantidadAnulada(), l.getFechaVencimiento(),
                    l.getFechaVencimiento() == null ? null : CalculadoraSemaforo.diasParaVencer(l.getFechaVencimiento(), hoy),
                    semaforo.calcular(l.getFechaVencimiento(), hoy), l.getEstado(), l.getFechaIngreso(),
                    l.getUbicacionAlmacen(), orden));
        }
        return lista;
    }

    /** El mismo criterio de FefoAllocator aplicado a la entidad Lote. */
    private static Comparator<Lote> ordenFefoLotes() {
        return Comparator.comparing((Lote l) -> new LoteCandidato(l.getId(), l.getFechaVencimiento(),
                l.getCantidadDisponible(), l.getFechaIngreso()), FefoAllocator.ORDEN_FEFO);
    }

    private String codigoLoteUnico() {
        for (int i = 0; i < 5; i++) {
            String codigo = Codigos.generar("LOT");
            if (!loteRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        return Codigos.generar("LOT", 10);
    }
}
