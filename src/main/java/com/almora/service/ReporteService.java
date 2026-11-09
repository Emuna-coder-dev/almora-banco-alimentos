package com.almora.service;

import com.almora.dto.comun.Pagina;
import com.almora.dto.reporte.IndicadoresResponse;
import com.almora.dto.reporte.ReporteRequest;
import com.almora.dto.reporte.ReporteResumenResponse;
import com.almora.dto.usuario.ImpactoDonanteResponse;
import com.almora.exception.ApiException;
import com.almora.fefo.CalculadoraSemaforo;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.auditoria.Reporte;
import com.almora.model.donacion.Confirmacion;
import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.donacion.ItemDonacion;
import com.almora.model.entrega.Entrega;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.inventario.EstadoLote;
import com.almora.model.inventario.Lote;
import com.almora.model.inventario.Semaforo;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.TipoRol;
import com.almora.repository.DonacionRepository;
import com.almora.repository.EntregaRepository;
import com.almora.repository.FundacionRepository;
import com.almora.repository.LoteRepository;
import com.almora.repository.ReporteRepository;
import com.almora.repository.UsuarioRepository;
import com.almora.security.SecurityUtils;
import com.almora.security.UsuarioAutenticado;
import com.almora.util.Textos;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.TreeMap;

/**
 * Reportes e indicadores (RF009, HU11, HU12, SCAMPER S1 y P1).
 *  - generar: crea el reporte filtrado por fechas y donante, y lo GUARDA para consultarlo despues (RNF-C01).
 *  - exportar a PDF o Excel: sale del reporte guardado, asi el archivo siempre coincide con lo mostrado
 *    y se puede repetir sin alterar ningun dato (HU12).
 *  - indicadores: tablero con el % de producto entregado antes de vencer (indicador principal, SCAMPER S1).
 *  - impacto: lo que lograron las donaciones de un donante (SCAMPER P1).
 */
@Service
public class ReporteService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA);
    private static final Color MORADO = new Color(0x4A, 0x1A, 0x6B);

    private final ReporteRepository repositorio;
    private final DonacionRepository donacionRepository;
    private final LoteRepository loteRepository;
    private final EntregaRepository entregaRepository;
    private final FundacionRepository fundacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;
    private final UsuarioService usuarioService;
    private final InventarioService inventario;
    private final CalculadoraSemaforo semaforo;
    private final AuditoriaService auditoria;

    public ReporteService(ReporteRepository repositorio, DonacionRepository donacionRepository,
                          LoteRepository loteRepository, EntregaRepository entregaRepository,
                          FundacionRepository fundacionRepository, UsuarioRepository usuarioRepository,
                          MongoTemplate mongoTemplate, UsuarioService usuarioService, InventarioService inventario,
                          CalculadoraSemaforo semaforo, AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.donacionRepository = donacionRepository;
        this.loteRepository = loteRepository;
        this.entregaRepository = entregaRepository;
        this.fundacionRepository = fundacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
        this.usuarioService = usuarioService;
        this.inventario = inventario;
        this.semaforo = semaforo;
        this.auditoria = auditoria;
    }

    // ================================================================ reportes (HU11)

    public Reporte generar(ReporteRequest req) {
        UsuarioAutenticado actor = SecurityUtils.actual();
        Criteria c = Criteria.where("fechaRegistro")
                .gte(req.desde().atStartOfDay(ZONA).toInstant())
                .lt(req.hasta().plusDays(1).atStartOfDay(ZONA).toInstant());
        String donanteNombre = null;
        if (!Textos.vacio(req.donanteId())) {
            Donante d = usuarioService.buscarDonante(req.donanteId());
            c.and("donanteId").is(d.getId());
            donanteNombre = d.getNombre();
        }
        List<Donacion> donaciones = mongoTemplate.find(Query.query(c)
                .with(org.springframework.data.domain.Sort.by("fechaRegistro")), Donacion.class);

        Reporte r = new Reporte();
        r.setFechaGeneracion(Instant.now());
        r.setGeneradoPor(actor.etiqueta());
        r.setDesde(req.desde());
        r.setHasta(req.hasta());
        r.setDonanteId(Textos.limpiar(req.donanteId()));
        r.setDonanteNombre(donanteNombre);

        Map<String, Long> porEstado = new TreeMap<>();
        Map<String, Long> porProducto = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        long unidades = 0;
        for (Donacion d : donaciones) {
            for (ItemDonacion i : d.getItems()) {
                Reporte.Fila f = new Reporte.Fila();
                f.setCodigoDonacion(d.getCodigo());
                f.setFecha(d.getFechaRegistro());
                f.setDonante(d.getDonanteNombre());
                f.setProducto(i.getNombreProducto());
                f.setCantidad(i.getCantidad());
                f.setUnidad(i.getUnidadMedida() == null ? "" : i.getUnidadMedida().name().toLowerCase());
                f.setEstado(d.getEstado().getTitulo());
                r.getFilas().add(f);
                porProducto.merge(i.getNombreProducto(), (long) i.getCantidad(), Long::sum);
            }
            porEstado.merge(d.getEstado().getTitulo(), (long) d.getTotalUnidades(), Long::sum);
            if (d.getEstado() != EstadoDonacion.ANULADA) {
                unidades += d.getTotalUnidades();
            }
        }
        r.setTotalDonaciones(donaciones.size());
        r.setTotalUnidades(unidades);
        porEstado.forEach((k, v) -> r.getUnidadesPorEstado().add(new Reporte.Total(k, v)));
        porProducto.forEach((k, v) -> r.getUnidadesPorProducto().add(new Reporte.Total(k, v)));

        Reporte guardado = repositorio.save(r);
        auditoria.registrar(AccionAuditoria.CREAR, "Reporte", guardado.getId(),
                req.desde() + " a " + req.hasta() + (donanteNombre == null ? "" : " / " + donanteNombre));
        return guardado;
    }

    public Pagina<ReporteResumenResponse> listar(int pagina, int tamano) {
        return Pagina.de(repositorio.findAllByOrderByFechaGeneracionDesc(PageRequest.of(pagina, tamano)),
                ReporteResumenResponse::de);
    }

    public Reporte obtener(String id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("Reporte no encontrado"));
    }

    // ================================================================ exportar (HU12)

    public byte[] exportarPdf(String id) {
        Reporte r = obtener(id);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Reporte de donaciones ALMORA");
            doc.open();
            Font titulo = new Font(Font.HELVETICA, 16, Font.BOLD, MORADO);
            Font normal = new Font(Font.HELVETICA, 10);
            Font negrita = new Font(Font.HELVETICA, 10, Font.BOLD);

            doc.add(new Paragraph("ALMORA - Reporte de donaciones", titulo));
            doc.add(new Paragraph("Periodo: " + r.getDesde() + " a " + r.getHasta()
                    + (r.getDonanteNombre() == null ? "" : "   |   Donante: " + r.getDonanteNombre()), normal));
            doc.add(new Paragraph("Generado el " + FECHA_HORA.format(r.getFechaGeneracion()) + " por " + r.getGeneradoPor(), normal));
            Paragraph tot = new Paragraph("Donaciones: " + r.getTotalDonaciones() + "    Unidades (sin anuladas): "
                    + r.getTotalUnidades(), negrita);
            tot.setSpacingAfter(10);
            doc.add(tot);

            PdfPTable t = new PdfPTable(new float[]{2.2f, 2f, 3.2f, 3.2f, 1.3f, 1.4f, 2f});
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            for (String h : new String[]{"Codigo", "Fecha", "Donante", "Producto", "Cantidad", "Unidad", "Estado"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                c.setBackgroundColor(MORADO);
                c.setPadding(4);
                t.addCell(c);
            }
            Font celda = new Font(Font.HELVETICA, 9);
            for (Reporte.Fila f : r.getFilas()) {
                t.addCell(new Phrase(f.getCodigoDonacion(), celda));
                t.addCell(new Phrase(f.getFecha() == null ? "" : FECHA_HORA.format(f.getFecha()), celda));
                t.addCell(new Phrase(f.getDonante(), celda));
                t.addCell(new Phrase(f.getProducto(), celda));
                PdfPCell cant = new PdfPCell(new Phrase(String.valueOf(f.getCantidad()), celda));
                cant.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(cant);
                t.addCell(new Phrase(f.getUnidad(), celda));
                t.addCell(new Phrase(f.getEstado(), celda));
            }
            doc.add(t);

            Paragraph h2 = new Paragraph("Unidades por producto", negrita);
            h2.setSpacingBefore(12);
            doc.add(h2);
            for (Reporte.Total x : r.getUnidadesPorProducto()) {
                doc.add(new Paragraph(x.clave() + ": " + x.unidades(), normal));
            }
            h2 = new Paragraph("Unidades por estado", negrita);
            h2.setSpacingBefore(8);
            doc.add(h2);
            for (Reporte.Total x : r.getUnidadesPorEstado()) {
                doc.add(new Paragraph(x.clave() + ": " + x.unidades(), normal));
            }
            doc.close();
        } catch (DocumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF");
        }
        auditoria.registrar(AccionAuditoria.EXPORTAR, "Reporte", id, "PDF");
        return out.toByteArray();
    }

    public byte[] exportarExcel(String id) {
        Reporte r = obtener(id);
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle encabezado = libro.createCellStyle();
            var fuente = libro.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            encabezado.setFont(fuente);
            encabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet hoja = libro.createSheet("Donaciones");
            int fila = 0;
            hoja.createRow(fila++).createCell(0).setCellValue("ALMORA - Reporte de donaciones");
            hoja.createRow(fila++).createCell(0).setCellValue("Periodo: " + r.getDesde() + " a " + r.getHasta()
                    + (r.getDonanteNombre() == null ? "" : " | Donante: " + r.getDonanteNombre()));
            hoja.createRow(fila++).createCell(0).setCellValue("Generado: " + FECHA_HORA.format(r.getFechaGeneracion())
                    + " por " + r.getGeneradoPor());
            fila++;
            String[] titulos = {"Codigo", "Fecha", "Donante", "Producto", "Cantidad", "Unidad", "Estado"};
            Row h = hoja.createRow(fila++);
            for (int i = 0; i < titulos.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(titulos[i]);
                c.setCellStyle(encabezado);
            }
            for (Reporte.Fila f : r.getFilas()) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(f.getCodigoDonacion());
                row.createCell(1).setCellValue(f.getFecha() == null ? "" : FECHA_HORA.format(f.getFecha()));
                row.createCell(2).setCellValue(f.getDonante());
                row.createCell(3).setCellValue(f.getProducto());
                row.createCell(4).setCellValue(f.getCantidad());
                row.createCell(5).setCellValue(f.getUnidad());
                row.createCell(6).setCellValue(f.getEstado());
            }
            fila++;
            hoja.createRow(fila++).createCell(0).setCellValue("Total donaciones: " + r.getTotalDonaciones());
            hoja.createRow(fila).createCell(0).setCellValue("Total unidades (sin anuladas): " + r.getTotalUnidades());
            // Anchos fijos (en caracteres): autoSizeColumn necesita fuentes del sistema que algunos servidores no tienen
            int[] anchos = {20, 18, 32, 30, 10, 12, 22};
            for (int i = 0; i < anchos.length; i++) {
                hoja.setColumnWidth(i, anchos[i] * 256);
            }

            Sheet resumen = libro.createSheet("Resumen");
            int rf = 0;
            Row t1 = resumen.createRow(rf++);
            t1.createCell(0).setCellValue("Producto");
            t1.createCell(1).setCellValue("Unidades");
            t1.getCell(0).setCellStyle(encabezado);
            t1.getCell(1).setCellStyle(encabezado);
            for (Reporte.Total x : r.getUnidadesPorProducto()) {
                Row row = resumen.createRow(rf++);
                row.createCell(0).setCellValue(x.clave());
                row.createCell(1).setCellValue(x.unidades());
            }
            rf++;
            Row t2 = resumen.createRow(rf++);
            t2.createCell(0).setCellValue("Estado");
            t2.createCell(1).setCellValue("Unidades");
            t2.getCell(0).setCellStyle(encabezado);
            t2.getCell(1).setCellStyle(encabezado);
            for (Reporte.Total x : r.getUnidadesPorEstado()) {
                Row row = resumen.createRow(rf++);
                row.createCell(0).setCellValue(x.clave());
                row.createCell(1).setCellValue(x.unidades());
            }
            resumen.setColumnWidth(0, 32 * 256);
            resumen.setColumnWidth(1, 12 * 256);

            libro.write(out);
            auditoria.registrar(AccionAuditoria.EXPORTAR, "Reporte", id, "Excel");
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel");
        }
    }

    // ================================================================ indicadores (SCAMPER S1)

    public IndicadoresResponse indicadores() {
        LocalDate hoy = inventario.hoy();
        long recibidas = 0, enBodega = 0, asignadas = 0, entregadas = 0, vencidas = 0, rojo = 0, amarillo = 0;
        Map<String, Long> recibidasPorCategoria = new TreeMap<>();
        Map<String, Long> bodegaPorCategoria = new TreeMap<>();
        for (Lote l : loteRepository.findAll()) {
            if (l.getEstado() == EstadoLote.ANULADO) {
                continue;
            }
            String cat = l.getCategoria() == null ? "OTRO" : l.getCategoria().name();
            recibidasPorCategoria.merge(cat, (long) l.getCantidadInicial(), Long::sum);
            if (l.getCantidadDisponible() > 0) {
                bodegaPorCategoria.merge(cat, (long) l.getCantidadDisponible(), Long::sum);
            }
            recibidas += l.getCantidadInicial();
            enBodega += l.getCantidadDisponible();
            asignadas += l.getCantidadAsignada();
            entregadas += l.getCantidadEntregada();
            vencidas += l.getCantidadVencida();
            if (l.getEstado() == EstadoLote.DISPONIBLE && l.getCantidadDisponible() > 0) {
                Semaforo s = semaforo.calcular(l.getFechaVencimiento(), hoy);
                if (s == Semaforo.ROJO) {
                    rojo++;
                } else if (s == Semaforo.AMARILLO) {
                    amarillo++;
                }
            }
        }
        double porcentaje = entregadas + vencidas == 0 ? 0
                : Math.round(entregadas * 1000.0 / (entregadas + vencidas)) / 10.0;

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoDonacion e : EstadoDonacion.values()) {
            porEstado.put(e.name(), donacionRepository.countByEstado(e));
        }
        Set<String> fundacionesAtendidas = new LinkedHashSet<>();
        for (Entrega e : entregaRepository.findByEstado(EstadoEntrega.ENTREGADA)) {
            fundacionesAtendidas.add(e.getFundacionId());
        }
        long fallidas = mongoTemplate.count(Query.query(Criteria.where("confirmacion.estado").is(Confirmacion.Estado.FALLIDA)),
                Donacion.class);
        return new IndicadoresResponse(
                usuarioRepository.countByRolAndActivoTrue(TipoRol.DONANTE),
                fundacionRepository.countByActivaTrue(),
                donacionRepository.count(),
                porEstado, recibidas, enBodega, asignadas, entregadas, vencidas, porcentaje, rojo, amarillo,
                entregaRepository.countByEstado(EstadoEntrega.ENTREGADA),
                fundacionesAtendidas.size(), fallidas, recibidasPorCategoria, bodegaPorCategoria);
    }

    // ================================================================ impacto del donante (SCAMPER P1)

    public ImpactoDonanteResponse impacto(String donanteId) {
        List<Donacion> donaciones = donacionRepository.findByDonanteId(donanteId);
        List<String> ids = new ArrayList<>();
        long donadas = 0, certificados = 0;
        for (Donacion d : donaciones) {
            if (d.getEstado() != EstadoDonacion.ANULADA) {
                donadas += d.getTotalUnidades();
            }
            if (d.getEstado().permiteCertificado()) {
                certificados++;
            }
            ids.add(d.getId());
        }
        long entregadas = 0, bodega = 0;
        if (!ids.isEmpty()) {
            for (Lote l : loteRepository.findByDonacionIdIn(ids)) {
                entregadas += l.getCantidadEntregada();
                bodega += l.getCantidadDisponible();
            }
        }
        Set<String> fundaciones = new LinkedHashSet<>();
        if (!ids.isEmpty()) {
            Set<String> mias = new java.util.HashSet<>(ids);
            for (Entrega e : entregaRepository.findByItemsDonacionIdIn(ids)) {
                boolean llevaMias = e.getItems().stream().anyMatch(i -> mias.contains(i.getDonacionId()));
                if (e.getEstado() == EstadoEntrega.ENTREGADA && llevaMias) {
                    fundaciones.add(e.getFundacionNombre());
                }
            }
        }
        return new ImpactoDonanteResponse(donaciones.size(), donadas, entregadas, bodega, List.copyOf(fundaciones), certificados);
    }
}
