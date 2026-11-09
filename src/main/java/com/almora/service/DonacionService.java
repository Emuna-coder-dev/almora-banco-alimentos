package com.almora.service;

import com.almora.dto.comun.Pagina;
import com.almora.dto.donacion.ActualizarDonacionRequest;
import com.almora.dto.donacion.DestinoResponse;
import com.almora.dto.donacion.DonacionRequest;
import com.almora.dto.donacion.DonacionResponse;
import com.almora.dto.donacion.ItemDonacionRequest;
import com.almora.dto.donacion.ItemDonacionResponse;
import com.almora.dto.donacion.PasoRuta;
import com.almora.dto.donacion.ProgresoDonacion;
import com.almora.dto.donacion.SeguimientoResponse;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.donacion.Anulacion;
import com.almora.model.donacion.Confirmacion;
import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.donacion.EventoSeguimiento;
import com.almora.model.donacion.ItemDonacion;
import com.almora.model.entrega.Entrega;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.entrega.ItemEntrega;
import com.almora.model.inventario.Lote;
import com.almora.model.inventario.Producto;
import com.almora.model.usuario.Donante;
import com.almora.repository.DonacionRepository;
import com.almora.repository.EntregaRepository;
import com.almora.repository.LoteRepository;
import com.almora.security.SecurityUtils;
import com.almora.security.UsuarioAutenticado;
import com.almora.util.Codigos;
import com.almora.util.Textos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Donaciones (RF004-RF007, HU05-HU08) y su seguimiento tipo guia de mensajeria.
 *
 *  - Un DONANTE registra su donacion desde el portal: queda PENDIENTE hasta que el banco la recibe.
 *  - Un ADMIN registra la donacion a nombre de un donante: entra RECIBIDA y se suma de una vez al inventario.
 *  - Al recibirse, cada producto se convierte en un Lote con su fecha de vencimiento (SCAMPER S2).
 *  - Despues avanza: ALMACENADA -> ASIGNADA -> EN_RUTA -> ENTREGADA (lo calculan las entregas).
 *  - No se borra: se ANULA con motivo, fecha y responsable (SCAMPER R2) y queda en auditoria.
 */
@Service
public class DonacionService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final DonacionRepository repositorio;
    private final LoteRepository loteRepository;
    private final EntregaRepository entregaRepository;
    private final MongoTemplate mongoTemplate;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final InventarioService inventario;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public DonacionService(DonacionRepository repositorio, LoteRepository loteRepository,
                           EntregaRepository entregaRepository, MongoTemplate mongoTemplate,
                           ProductoService productoService, UsuarioService usuarioService,
                           InventarioService inventario, AuditoriaService auditoria,
                           ApplicationEventPublisher eventos) {
        this.repositorio = repositorio;
        this.loteRepository = loteRepository;
        this.entregaRepository = entregaRepository;
        this.mongoTemplate = mongoTemplate;
        this.productoService = productoService;
        this.usuarioService = usuarioService;
        this.inventario = inventario;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    // ================================================================ registrar (HU05)

    @Transactional
    public DonacionResponse registrar(DonacionRequest req) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Donante donante;
        if (actor.esDonante()) {
            donante = usuarioService.buscarDonante(actor.id());
        } else {
            if (Textos.vacio(req.donanteId())) {
                throw ApiException.solicitudInvalida("Indica el donante (donanteId) al que pertenece la donacion");
            }
            donante = usuarioService.buscarDonante(req.donanteId());
        }
        if (!donante.isActivo()) {
            throw ApiException.solicitudInvalida("El donante esta desactivado");
        }

        Donacion d = new Donacion();
        d.setCodigo(codigoDonacionUnico());
        d.setDonanteId(donante.getId());
        d.setDonanteNombre(donante.getNombre());
        d.setDonanteCorreo(donante.getCorreo());
        d.setItems(construirItems(req.items()));
        d.recalcularTotal();
        d.setObservaciones(Textos.limpiar(req.observaciones()));
        d.setRegistradaPor(actor.etiqueta());
        d.setFechaRegistro(Instant.now());
        d.setEstado(EstadoDonacion.PENDIENTE);
        d.agregarEvento(EventoSeguimiento.de(EstadoDonacion.PENDIENTE.name(), "Donacion registrada",
                d.getTotalUnidades() + " unidad(es) en " + d.getItems().size() + " producto(s)",
                null, actor.etiqueta(), d.getCodigo()));
        d = repositorio.save(d);

        if (actor.esAdministrativo()) {
            // El administrador la registra cuando ya la tiene en sus manos: entra directo al inventario
            recibirInterno(d, req.ubicacionAlmacen(), actor);
        }
        auditoria.registrar(AccionAuditoria.CREAR, "Donacion", d.getId(),
                d.getCodigo() + " de " + donante.getCorreo() + " (" + d.getTotalUnidades() + " und)");
        return aRespuesta(d);
    }

    /** Edicion de una donacion PENDIENTE (aun no esta en el inventario). RF007. */
    @Transactional
    public DonacionResponse actualizar(String id, ActualizarDonacionRequest req) {
        Donacion d = buscarConPermiso(id);
        if (d.getEstado() != EstadoDonacion.PENDIENTE) {
            throw ApiException.conflicto("Solo se pueden editar los productos de una donacion PENDIENTE. "
                    + "Si ya entro al inventario, anulala y registrala de nuevo.");
        }
        d.setItems(construirItems(req.items()));
        d.recalcularTotal();
        d.setObservaciones(Textos.limpiar(req.observaciones()));
        d.agregarEvento(EventoSeguimiento.de(EstadoDonacion.PENDIENTE.name(), "Donacion modificada",
                "Nuevo total: " + d.getTotalUnidades() + " unidad(es)", null, SecurityUtils.actual().etiqueta(), null));
        d = repositorio.save(d);
        auditoria.registrar(AccionAuditoria.ACTUALIZAR, "Donacion", id, d.getCodigo());
        return aRespuesta(d);
    }

    // ================================================================ estados manuales (HU08)

    @Transactional
    public DonacionResponse cambiarEstado(String id, EstadoDonacion nuevo, String ubicacion, String observacion) {
        Donacion d = buscar(id);
        if (!d.getEstado().transicionesManuales().contains(nuevo)) {
            throw ApiException.conflicto("No se puede pasar de " + d.getEstado() + " a " + nuevo + ". "
                    + "Permitido desde " + d.getEstado() + ": " + d.getEstado().transicionesManuales()
                    + ". ASIGNADA, EN_RUTA y ENTREGADA se actualizan solas con las entregas; ANULADA tiene su propia accion.");
        }
        UsuarioAutenticado actor = SecurityUtils.actual();
        if (nuevo == EstadoDonacion.RECIBIDA) {
            recibirInterno(d, ubicacion, actor);
        } else if (nuevo == EstadoDonacion.ALMACENADA) {
            String ubic = Textos.limpiar(ubicacion);
            if (ubic == null) {
                ubic = d.getUbicacionAlmacen() == null ? "Bodega principal" : d.getUbicacionAlmacen();
            }
            d.setEstado(EstadoDonacion.ALMACENADA);
            d.setFechaAlmacenamiento(Instant.now());
            d.setUbicacionAlmacen(ubic);
            d.agregarEvento(EventoSeguimiento.de(EstadoDonacion.ALMACENADA.name(), "Clasificada y almacenada",
                    observacion == null ? "Productos clasificados por lote y fecha de vencimiento" : observacion,
                    ubic, actor.etiqueta(), null));
            inventario.actualizarUbicacion(d.getId(), ubic);
            d = repositorio.save(d);
        }
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, "Donacion", id, d.getCodigo() + " -> " + nuevo);
        return aRespuesta(d);
    }

    /** PENDIENTE -> RECIBIDA: crea los lotes (entra al inventario) y dispara el correo de confirmacion. */
    private void recibirInterno(Donacion d, String ubicacion, UsuarioAutenticado actor) {
        LocalDate hoy = inventario.hoy();
        for (ItemDonacion item : d.getItems()) {
            if (item.getFechaVencimiento() != null && item.getFechaVencimiento().isBefore(hoy)) {
                throw ApiException.solicitudInvalida("El producto '" + item.getNombreProducto()
                        + "' ya esta vencido (" + item.getFechaVencimiento() + "). Corrige la donacion antes de recibirla.");
            }
        }
        String ubic = Textos.limpiar(ubicacion);
        for (ItemDonacion item : d.getItems()) {
            Lote lote = inventario.crearLote(d, item, ubic);
            item.setLoteId(lote.getId());
            item.setCodigoLote(lote.getCodigo());
        }
        d.setEstado(EstadoDonacion.RECIBIDA);
        d.setFechaRecepcion(Instant.now());
        d.setUbicacionAlmacen(ubic);
        d.setConfirmacion(new Confirmacion(Confirmacion.Estado.PENDIENTE, Instant.now(), "En cola de envio"));
        d.agregarEvento(EventoSeguimiento.de(EstadoDonacion.RECIBIDA.name(), "Recibida en el banco de alimentos",
                "Los productos ingresaron al inventario en " + d.getItems().size() + " lote(s)",
                ubic, actor.etiqueta(), null));
        repositorio.save(d);
        eventos.publishEvent(new DonacionRecibidaEvent(d.getId()));
    }

    // ================================================================ anular (RF006 / HU07)

    @Transactional
    public DonacionResponse anular(String id, String motivo) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Donacion d = buscarConPermiso(id);
        if (actor.esDonante() && d.getEstado() != EstadoDonacion.PENDIENTE) {
            throw ApiException.prohibido("Solo puedes anular tus donaciones que aun estan pendientes. "
                    + "Para las demas, comunicate con la fundacion.");
        }
        if (!d.getEstado().puedeAnularse()) {
            throw ApiException.conflicto("Una donacion en estado " + d.getEstado() + " no se puede anular");
        }
        if (d.getEstado() != EstadoDonacion.PENDIENTE) {
            inventario.anularLotesDeDonacion(d.getId());   // resta del inventario una sola vez
        }
        String m = Textos.limpiar(motivo);
        d.setAnulacion(new Anulacion(m, Instant.now(), actor.etiqueta()));
        d.setEstado(EstadoDonacion.ANULADA);
        d.agregarEvento(EventoSeguimiento.de(EstadoDonacion.ANULADA.name(), "Donacion anulada", m, null, actor.etiqueta(), null));
        d = repositorio.save(d);
        auditoria.registrar(AccionAuditoria.ANULAR, "Donacion", id, d.getCodigo() + ": " + m);
        return aRespuesta(d);
    }

    // ================================================================ consultar

    public Pagina<DonacionResponse> listar(EstadoDonacion estado, String donanteId, LocalDate desde, LocalDate hasta,
                                           String codigo, Confirmacion.Estado confirmacion, int pagina, int tamano) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Criteria c = new Criteria();
        if (actor.esDonante()) {
            c.and("donanteId").is(actor.id());          // un donante solo ve las suyas
        } else if (!Textos.vacio(donanteId)) {
            c.and("donanteId").is(donanteId);
        }
        if (estado != null) {
            c.and("estado").is(estado);
        }
        if (!Textos.vacio(codigo)) {
            c.and("codigo").is(codigo.trim().toUpperCase());
        }
        if (confirmacion != null) {
            c.and("confirmacion.estado").is(confirmacion);
        }
        if (desde != null || hasta != null) {
            Criteria f = c.and("fechaRegistro");
            if (desde != null) {
                f.gte(desde.atStartOfDay(ZONA).toInstant());
            }
            if (hasta != null) {
                f.lt(hasta.plusDays(1).atStartOfDay(ZONA).toInstant());
            }
        }
        Query q = new Query(c);
        long total = mongoTemplate.count(q, Donacion.class);
        q.with(Sort.by(Sort.Direction.DESC, "fechaRegistro")).skip((long) pagina * tamano).limit(tamano);
        List<DonacionResponse> lista = mongoTemplate.find(q, Donacion.class).stream().map(this::aRespuesta).toList();
        return new Pagina<>(lista, pagina, tamano, total, (int) Math.ceil((double) total / tamano));
    }

    public DonacionResponse obtener(String id) {
        return aRespuesta(buscarConPermiso(id));
    }

    // ================================================================ seguimiento (todos los roles)

    public SeguimientoResponse seguimientoPorId(String id) {
        return seguimiento(buscarConPermiso(id));
    }

    public SeguimientoResponse seguimientoPorCodigo(String codigo) {
        Donacion d = repositorio.findByCodigo(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> ApiException.noEncontrado("No existe una donacion con ese codigo"));
        verificarPermiso(d);
        return seguimiento(d);
    }

    private SeguimientoResponse seguimiento(Donacion d) {
        List<Lote> lotes = loteRepository.findByDonacionId(d.getId());
        List<Entrega> entregas = entregaRepository.findByItemsDonacionId(d.getId());
        ProgresoDonacion progreso = calcularProgreso(d, lotes, entregas);

        List<DestinoResponse> destinos = new ArrayList<>();
        for (Entrega e : entregas) {
            int unidades = e.getItems().stream().filter(i -> d.getId().equals(i.getDonacionId()))
                    .mapToInt(ItemEntrega::getCantidad).sum();
            destinos.add(new DestinoResponse(e.getCodigo(), e.getFundacionNombre(), e.getDireccionDestino(),
                    e.getCiudadDestino(), e.getEstado(), e.getEstado().getTitulo(), unidades, e.getFechaProgramada(),
                    e.getFechaEntrega(), e.getReceptor()));
        }
        return new SeguimientoResponse(d.getId(), d.getCodigo(), d.getDonanteNombre(), d.getEstado(),
                d.getEstado().getTitulo(), d.getEstado().getDescripcion(), progreso, construirRuta(d),
                d.getSeguimiento(), destinos);
    }

    /** Los 6 pasos fijos de la "guia", marcando cuales ya se cumplieron y cual es el actual. */
    private List<PasoRuta> construirRuta(Donacion d) {
        List<EstadoDonacion> pasos = List.of(EstadoDonacion.PENDIENTE, EstadoDonacion.RECIBIDA,
                EstadoDonacion.ALMACENADA, EstadoDonacion.ASIGNADA, EstadoDonacion.EN_RUTA, EstadoDonacion.ENTREGADA);
        // Primera fecha en que ocurrio cada estado
        Map<String, Instant> fechas = new HashMap<>();
        for (EventoSeguimiento ev : d.getSeguimiento()) {
            fechas.putIfAbsent(ev.getEstado(), ev.getFecha());
        }
        EstadoDonacion actual = d.getEstado();
        int indiceActual = switch (actual) {
            case PENDIENTE -> 0;
            case RECIBIDA -> 1;
            case ALMACENADA -> 2;
            case ASIGNADA -> 3;
            case EN_RUTA -> 4;
            case PARCIALMENTE_ENTREGADA, ENTREGADA -> 5;
            case VENCIDA, ANULADA -> -1;
        };
        List<PasoRuta> ruta = new ArrayList<>();
        for (int i = 0; i < pasos.size(); i++) {
            EstadoDonacion p = pasos.get(i);
            String titulo = p == EstadoDonacion.PENDIENTE ? "Registrada" : p.getTitulo();
            Instant fecha = fechas.get(p.name());
            if (p == EstadoDonacion.ENTREGADA && fecha == null) {
                fecha = fechas.get(EstadoDonacion.PARCIALMENTE_ENTREGADA.name());
            }
            boolean completado = indiceActual >= 0 ? i <= indiceActual : fecha != null;
            boolean esActual = i == indiceActual;
            ruta.add(new PasoRuta(p.name(), titulo, p.getDescripcion(), completado, esActual, fecha));
        }
        if (actual == EstadoDonacion.VENCIDA || actual == EstadoDonacion.ANULADA) {
            ruta.add(new PasoRuta(actual.name(), actual.getTitulo(), actual.getDescripcion(), true, true,
                    fechas.get(actual.name())));
        }
        return ruta;
    }

    private ProgresoDonacion calcularProgreso(Donacion d, List<Lote> lotes, List<Entrega> entregas) {
        int total = d.getTotalUnidades();
        if (lotes.isEmpty()) {
            return new ProgresoDonacion(total, 0, 0, 0, 0, 0, d.getEstado() == EstadoDonacion.ANULADA ? total : 0, 0);
        }
        int disponible = 0, asignada = 0, entregada = 0, vencida = 0, anulada = 0;
        for (Lote l : lotes) {
            disponible += l.getCantidadDisponible();
            asignada += l.getCantidadAsignada();
            entregada += l.getCantidadEntregada();
            vencida += l.getCantidadVencida();
            anulada += l.getCantidadAnulada();
        }
        int enRuta = 0;
        for (Entrega e : entregas) {
            if (e.getEstado() == EstadoEntrega.EN_RUTA) {
                enRuta += e.getItems().stream().filter(i -> d.getId().equals(i.getDonacionId()))
                        .mapToInt(ItemEntrega::getCantidad).sum();
            }
        }
        int porcentaje = total == 0 ? 0 : (int) Math.round(entregada * 100.0 / total);
        return new ProgresoDonacion(total, disponible, Math.max(0, asignada - enRuta), enRuta, entregada, vencida, anulada, porcentaje);
    }

    /**
     * Recalcula el estado de una donacion a partir de sus lotes y entregas.
     * Lo llaman EntregaService (al programar, despachar, entregar o cancelar) y la tarea de vencimientos.
     */
    public void recalcularEstado(String donacionId) {
        Donacion d = repositorio.findById(donacionId).orElse(null);
        if (d == null || d.getEstado() == EstadoDonacion.PENDIENTE || d.getEstado() == EstadoDonacion.ANULADA) {
            return;
        }
        ProgresoDonacion p = calcularProgreso(d, loteRepository.findByDonacionId(donacionId),
                entregaRepository.findByItemsDonacionId(donacionId));
        EstadoDonacion base = d.getFechaAlmacenamiento() != null ? EstadoDonacion.ALMACENADA : EstadoDonacion.RECIBIDA;
        EstadoDonacion nuevo;
        if (p.total() > 0 && p.entregadas() == p.total()) {
            nuevo = EstadoDonacion.ENTREGADA;
        } else if (p.enRuta() > 0) {
            nuevo = EstadoDonacion.EN_RUTA;
        } else if (p.asignadas() > 0) {
            nuevo = EstadoDonacion.ASIGNADA;
        } else if (p.entregadas() > 0) {
            nuevo = EstadoDonacion.PARCIALMENTE_ENTREGADA;
        } else if (p.vencidas() > 0 && p.enBodega() == 0) {
            nuevo = EstadoDonacion.VENCIDA;
        } else {
            nuevo = base;
        }
        if (nuevo != d.getEstado()) {
            d.setEstado(nuevo);
            if (nuevo == EstadoDonacion.VENCIDA) {
                d.agregarEvento(EventoSeguimiento.de(nuevo.name(), "Productos vencidos",
                        p.vencidas() + " unidad(es) vencieron antes de poder entregarse", d.getUbicacionAlmacen(),
                        "Sistema", null));
            }
            repositorio.save(d);
        }
    }

    /** Agrega un evento a la ruta de varias donaciones (lo usa EntregaService). */
    public void agregarEvento(Set<String> donacionIds, EventoSeguimiento evento) {
        for (String id : donacionIds) {
            repositorio.findById(id).ifPresent(d -> {
                d.agregarEvento(evento);
                repositorio.save(d);
            });
        }
    }

    // ================================================================ apoyo

    public Donacion buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Donacion no encontrada"));
    }

    /** Un donante solo puede ver o tocar sus propias donaciones. */
    public Donacion buscarConPermiso(String id) {
        Donacion d = buscar(id);
        verificarPermiso(d);
        return d;
    }

    private void verificarPermiso(Donacion d) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        if (actor.esDonante() && !actor.id().equals(d.getDonanteId())) {
            // Se responde 404 para no revelar que existe una donacion de otra persona
            throw ApiException.noEncontrado("Donacion no encontrada");
        }
    }

    public void guardar(Donacion d) {
        repositorio.save(d);
    }

    private List<ItemDonacion> construirItems(List<ItemDonacionRequest> solicitud) {
        LocalDate hoy = inventario.hoy();
        List<ItemDonacion> items = new ArrayList<>();
        for (ItemDonacionRequest r : solicitud) {
            Producto p = productoService.buscar(r.productoId());
            if (!p.isActivo()) {
                throw ApiException.solicitudInvalida("El producto '" + p.getNombre() + "' no esta activo en el catalogo");
            }
            if (p.isPerecedero() && r.fechaVencimiento() == null) {
                throw ApiException.solicitudInvalida("'" + p.getNombre() + "' es perecedero: la fecha de vencimiento es obligatoria");
            }
            if (r.fechaVencimiento() != null && r.fechaVencimiento().isBefore(hoy)) {
                throw ApiException.solicitudInvalida("'" + p.getNombre() + "' ya esta vencido (" + r.fechaVencimiento()
                        + "). No se reciben productos vencidos.");
            }
            ItemDonacion i = new ItemDonacion();
            i.setProductoId(p.getId());
            i.setNombreProducto(p.getNombre());
            i.setCategoria(p.getCategoria());
            i.setPerecedero(p.isPerecedero());
            i.setUnidadMedida(p.getUnidadMedida());
            i.setCantidad(r.cantidad());
            i.setFechaVencimiento(r.fechaVencimiento());
            items.add(i);
        }
        return items;
    }

    public DonacionResponse aRespuesta(Donacion d) {
        List<ItemDonacionResponse> items = d.getItems().stream().map(i -> new ItemDonacionResponse(
                i.getProductoId(), i.getNombreProducto(), i.getCategoria(), i.isPerecedero(), i.getUnidadMedida(),
                i.getCantidad(), i.getFechaVencimiento(), i.getCodigoLote(),
                inventario.semaforoDe(i.getFechaVencimiento()))).toList();
        return new DonacionResponse(d.getId(), d.getCodigo(), d.getDonanteId(), d.getDonanteNombre(),
                d.getDonanteCorreo(), items, d.getTotalUnidades(), d.getEstado(), d.getEstado().getTitulo(),
                d.getFechaRegistro(), d.getFechaRecepcion(), d.getFechaAlmacenamiento(), d.getUbicacionAlmacen(),
                d.getRegistradaPor(), d.getObservaciones(), d.getConfirmacion(), d.getEstado().permiteCertificado(),
                d.getCodigoCertificado(), d.getAnulacion());
    }

    private String codigoDonacionUnico() {
        for (int i = 0; i < 5; i++) {
            String codigo = Codigos.generar("DON");
            if (!repositorio.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        return Codigos.generar("DON", 10);
    }
}
