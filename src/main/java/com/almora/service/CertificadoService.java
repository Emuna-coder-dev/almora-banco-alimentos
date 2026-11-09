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
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Certificado de donacion en PDF (SCAMPER C1): que, cuanto y cuando dono, con codigo de verificacion.
 * Cualquiera puede comprobar que es autentico en /api/publico/certificados/{codigo}.
 *
 * Nota: antes de usarlo como soporte tributario, la fundacion debe validar los requisitos formales
 * (por ejemplo, los del certificado de donacion del Estatuto Tributario) con su contador.
 */
@Service
public class CertificadoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Bogota"));
    private static final Color MORADO = new Color(0x4A, 0x1A, 0x6B);
    private static final Color GRIS = new Color(0xF2, 0xF0, 0xF5);

    public byte[] generar(Donacion d, Rol donante, String urlVerificacion) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 54, 54, 54, 54);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Certificado de donacion " + d.getCodigo());
            doc.addAuthor("Banco de alimentos ALMORA");
            doc.open();

            Font titulo = new Font(Font.HELVETICA, 20, Font.BOLD, MORADO);
            Font sub = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
            Font negrita = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
            Font pequena = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            Paragraph p = new Paragraph("BANCO DE ALIMENTOS ALMORA", sub);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            p = new Paragraph("CERTIFICADO DE DONACION", titulo);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(6);
            p.setSpacingAfter(18);
            doc.add(p);

            String identificacion = donante instanceof Donante dn && dn.getIdentificacion() != null
                    ? ", identificado(a) con " + dn.getIdentificacion() : "";
            Instant fecha = d.getFechaRecepcion() != null ? d.getFechaRecepcion() : d.getFechaRegistro();
            Paragraph cuerpo = new Paragraph();
            cuerpo.setLeading(16);
            cuerpo.add(new Phrase("El Banco de Alimentos ALMORA certifica que ", normal));
            cuerpo.add(new Phrase(d.getDonanteNombre(), negrita));
            cuerpo.add(new Phrase(identificacion + ", realizo el " + FECHA.format(fecha)
                    + " una donacion en especie registrada con el codigo ", normal));
            cuerpo.add(new Phrase(d.getCodigo(), negrita));
            cuerpo.add(new Phrase(", compuesta por los siguientes productos:", normal));
            cuerpo.setSpacingAfter(14);
            doc.add(cuerpo);

            PdfPTable tabla = new PdfPTable(new float[]{4.5f, 1.6f, 1.8f, 2.1f});
            tabla.setWidthPercentage(100);
            for (String h : new String[]{"Producto", "Cantidad", "Unidad", "Vencimiento"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
                c.setBackgroundColor(MORADO);
                c.setPadding(6);
                tabla.addCell(c);
            }
            boolean par = false;
            for (ItemDonacion i : d.getItems()) {
                Color fondo = par ? GRIS : Color.WHITE;
                tabla.addCell(celda(i.getNombreProducto(), fondo));
                tabla.addCell(celda(String.valueOf(i.getCantidad()), fondo));
                tabla.addCell(celda(i.getUnidadMedida() == null ? "-" : i.getUnidadMedida().name().toLowerCase(), fondo));
                tabla.addCell(celda(i.getFechaVencimiento() == null ? "No aplica" : i.getFechaVencimiento().toString(), fondo));
                par = !par;
            }
            PdfPCell total = new PdfPCell(new Phrase("Total de unidades: " + d.getTotalUnidades(), negrita));
            total.setColspan(4);
            total.setPadding(6);
            total.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(total);
            doc.add(tabla);

            Paragraph estado = new Paragraph("Estado actual de la donacion: " + d.getEstado().getTitulo(), normal);
            estado.setSpacingBefore(14);
            doc.add(estado);

            Paragraph verif = new Paragraph();
            verif.setSpacingBefore(28);
            verif.add(new Phrase("Codigo de verificacion: ", negrita));
            verif.add(new Phrase(d.getCodigoCertificado(), new Font(Font.COURIER, 12, Font.BOLD, MORADO)));
            doc.add(verif);
            doc.add(new Paragraph("Verifica la autenticidad de este certificado en: " + urlVerificacion, pequena));
            doc.add(new Paragraph("Expedido el " + FECHA.format(Instant.now())
                    + ". Documento generado por el sistema ALMORA; no requiere firma manuscrita.", pequena));
            doc.close();
        } catch (DocumentException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el certificado");
        }
        return out.toByteArray();
    }

    private static PdfPCell celda(String texto, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, new Font(Font.HELVETICA, 10)));
        c.setBackgroundColor(fondo);
        c.setPadding(5);
        return c;
    }
}
