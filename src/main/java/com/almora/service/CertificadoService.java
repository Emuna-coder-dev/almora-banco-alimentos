package com.almora.service;

import com.almora.exception.ApiException;
import com.almora.model.donacion.Donacion;
import com.almora.model.donacion.ItemDonacion;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.Rol;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Certificado de donacion en PDF (SCAMPER C1): que, cuanto y cuando dono, con codigo de verificacion.
 * Cualquiera puede comprobar que es autentico en /api/publico/certificados/{codigo}.
 *
 * Usa el isotipo y la paleta del manual de marca:
 *   Azul institucional #1B365D, verde esmeralda #10B981, azul dinamico #3B82F6 y blanco.
 *
 * Nota: antes de usarlo como soporte tributario, la fundacion debe validar los requisitos formales
 * (por ejemplo, los del certificado de donacion del Estatuto Tributario) con su contador.
 */
@Service
public class CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Bogota"));

    // Paleta corporativa
    private static final Color INSTITUCIONAL = new Color(0x1B, 0x36, 0x5D);
    private static final Color ESMERALDA = new Color(0x10, 0xB9, 0x81);
    private static final Color DINAMICO = new Color(0x3B, 0x82, 0xF6);
    private static final Color FILA_ALTERNA = new Color(0xEC, 0xFD, 0xF5);
    private static final Color GRIS_TEXTO = new Color(0x4A, 0x58, 0x73);

    /** Ruta del isotipo dentro del proyecto (src/main/resources/static/img/isotipo.png). */
    private static final String RUTA_ISOTIPO = "static/img/isotipo.png";

    public byte[] generar(Donacion d, Rol donante, String urlVerificacion) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 54, 54, 46, 54);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle("Certificado de donacion " + d.getCodigo());
            doc.addAuthor("Banco de alimentos ALMORA");
            doc.open();

            Font titulo = new Font(Font.HELVETICA, 21, Font.BOLD, INSTITUCIONAL);
            Font marca = new Font(Font.HELVETICA, 14, Font.BOLD, INSTITUCIONAL);
            Font lema = new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS_TEXTO);
            Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
            Font negrita = new Font(Font.HELVETICA, 11, Font.BOLD, INSTITUCIONAL);
            Font pequena = new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS_TEXTO);

            // ---------------------------------------------------------- encabezado: isotipo + nombre
            PdfPTable encabezado = new PdfPTable(new float[]{1.1f, 5f});
            encabezado.setWidthPercentage(100);
            PdfPCell celdaLogo = new PdfPCell();
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image isotipo = cargarIsotipo();
            if (isotipo != null) {
                isotipo.scaleToFit(64, 64);
                celdaLogo.addElement(isotipo);
            }
            encabezado.addCell(celdaLogo);

            PdfPCell celdaNombre = new PdfPCell();
            celdaNombre.setBorder(Rectangle.NO_BORDER);
            celdaNombre.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celdaNombre.addElement(new Paragraph("BANCO DE ALIMENTOS ALMORA", marca));
            celdaNombre.addElement(new Paragraph("Gestion de donaciones, inventario FEFO y distribucion a fundaciones", lema));
            encabezado.addCell(celdaNombre);
            doc.add(encabezado);

            // Franja con el degradado de la marca (verde esmeralda -> azul dinamico)
            dibujarFranja(writer.getDirectContent(), doc, writer.getVerticalPosition(false) - 8);

            Paragraph p = new Paragraph("CERTIFICADO DE DONACION", titulo);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(30);
            p.setSpacingAfter(20);
            doc.add(p);

            // ---------------------------------------------------------- cuerpo
            String identificacion = donante instanceof Donante dn && dn.getIdentificacion() != null
                    ? ", identificado(a) con " + dn.getIdentificacion() : "";
            Instant fecha = d.getFechaRecepcion() != null ? d.getFechaRecepcion() : d.getFechaRegistro();
            Paragraph cuerpo = new Paragraph();
            cuerpo.setLeading(17);
            cuerpo.add(new Phrase("El Banco de Alimentos ALMORA certifica que ", normal));
            cuerpo.add(new Phrase(d.getDonanteNombre(), negrita));
            cuerpo.add(new Phrase(identificacion + ", realizo el " + FECHA.format(fecha)
                    + " una donacion en especie registrada con el codigo ", normal));
            cuerpo.add(new Phrase(d.getCodigo(), negrita));
            cuerpo.add(new Phrase(", compuesta por los siguientes productos:", normal));
            cuerpo.setSpacingAfter(14);
            doc.add(cuerpo);

            // ---------------------------------------------------------- tabla de productos
            PdfPTable tabla = new PdfPTable(new float[]{4.5f, 1.6f, 1.8f, 2.1f});
            tabla.setWidthPercentage(100);
            for (String h : new String[]{"Producto", "Cantidad", "Unidad", "Vencimiento"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
                c.setBackgroundColor(INSTITUCIONAL);
                c.setBorderColor(INSTITUCIONAL);
                c.setPadding(7);
                tabla.addCell(c);
            }
            boolean par = false;
            for (ItemDonacion i : d.getItems()) {
                Color fondo = par ? FILA_ALTERNA : Color.WHITE;
                tabla.addCell(celda(i.getNombreProducto(), fondo));
                tabla.addCell(celda(String.valueOf(i.getCantidad()), fondo));
                tabla.addCell(celda(i.getUnidadMedida() == null ? "-" : i.getUnidadMedida().name().toLowerCase(), fondo));
                tabla.addCell(celda(i.getFechaVencimiento() == null ? "No aplica" : i.getFechaVencimiento().toString(), fondo));
                par = !par;
            }
            PdfPCell total = new PdfPCell(new Phrase("Total de unidades: " + d.getTotalUnidades(),
                    new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE)));
            total.setColspan(4);
            total.setPadding(7);
            total.setBackgroundColor(ESMERALDA);
            total.setBorderColor(ESMERALDA);
            total.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(total);
            doc.add(tabla);

            Paragraph estado = new Paragraph();
            estado.setSpacingBefore(14);
            estado.add(new Phrase("Estado actual de la donacion: ", normal));
            estado.add(new Phrase(d.getEstado().getTitulo(), negrita));
            doc.add(estado);

            // ---------------------------------------------------------- verificacion
            PdfPTable caja = new PdfPTable(1);
            caja.setWidthPercentage(100);
            caja.setSpacingBefore(28);
            PdfPCell cv = new PdfPCell();
            cv.setBorderColor(DINAMICO);
            cv.setBorderWidth(1.2f);
            cv.setPadding(12);
            Paragraph verif = new Paragraph();
            verif.add(new Phrase("Codigo de verificacion: ", negrita));
            verif.add(new Phrase(d.getCodigoCertificado(), new Font(Font.COURIER, 13, Font.BOLD, ESMERALDA)));
            cv.addElement(verif);
            Paragraph url = new Paragraph("Verifica la autenticidad de este certificado en: " + urlVerificacion, pequena);
            url.setSpacingBefore(4);
            cv.addElement(url);
            caja.addCell(cv);
            doc.add(caja);

            Paragraph pie = new Paragraph("Expedido el " + FECHA.format(Instant.now())
                    + ". Documento generado por el sistema ALMORA; no requiere firma manuscrita.", pequena);
            pie.setSpacingBefore(10);
            doc.add(pie);

            // Franja inferior con el degradado de la marca
            dibujarFranja(writer.getDirectContent(), doc, doc.bottom() - 18);
            doc.close();
        } catch (DocumentException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el certificado");
        }
        return out.toByteArray();
    }

    /** Carga el isotipo desde el classpath. Si no existe, el certificado se genera igual, sin logo. */
    private Image cargarIsotipo() {
        try (InputStream in = new ClassPathResource(RUTA_ISOTIPO).getInputStream()) {
            return Image.getInstance(in.readAllBytes());
        } catch (Exception e) {
            log.warn("No se encontro el isotipo en {}: el certificado sale sin logo", RUTA_ISOTIPO);
            return null;
        }
    }

    /** Dibuja una franja horizontal que pasa de verde esmeralda a azul dinamico. */
    private static void dibujarFranja(PdfContentByte cb, Document doc, float y) {
        float x0 = doc.left();
        float ancho = doc.right() - doc.left();
        int pasos = 60;
        float paso = ancho / pasos;
        for (int i = 0; i < pasos; i++) {
            float t = (float) i / (pasos - 1);
            Color c = new Color(
                    Math.round(ESMERALDA.getRed() + (DINAMICO.getRed() - ESMERALDA.getRed()) * t),
                    Math.round(ESMERALDA.getGreen() + (DINAMICO.getGreen() - ESMERALDA.getGreen()) * t),
                    Math.round(ESMERALDA.getBlue() + (DINAMICO.getBlue() - ESMERALDA.getBlue()) * t));
            cb.setColorFill(c);
            cb.rectangle(x0 + i * paso, y, paso + 0.6f, 4);
            cb.fill();
        }
    }

    private static PdfPCell celda(String texto, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, new Font(Font.HELVETICA, 10)));
        c.setBackgroundColor(fondo);
        c.setBorderColor(new Color(0xDB, 0xE4, 0xEF));
        c.setPadding(6);
        return c;
    }
}