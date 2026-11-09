package com.almora.service;

import com.almora.config.AlmoraProperties;
import com.almora.exception.ApiException;
import com.almora.model.auditoria.AccionAuditoria;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Copias de seguridad de la base de datos (RNF-BD01).
 *
 * Cada respaldo es un .zip con un archivo por coleccion (una linea JSON por documento, formato
 * Extended JSON de MongoDB). Se restaura con:  mongoimport --uri "..." --collection donaciones --file donaciones.json
 * Se ejecuta solo cada semana (configurable) y el superadministrador puede lanzarlo a mano.
 * Se conservan los ultimos N respaldos.
 *
 * Complemento recomendado: en Atlas M10 o superior, activar "Cloud Backup" (respaldos continuos administrados).
 */
@Service
public class RespaldoService {

    private static final Logger log = LoggerFactory.getLogger(RespaldoService.class);
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    /** Los tokens de sesion no se respaldan: no sirven para recuperar datos y son sensibles. */
    private static final Set<String> EXCLUIDAS = Set.of("refresh_tokens");

    public record InfoRespaldo(String archivo, long bytes, String fecha) {}

    private final MongoTemplate mongoTemplate;
    private final AlmoraProperties propiedades;
    private final AuditoriaService auditoria;

    public RespaldoService(MongoTemplate mongoTemplate, AlmoraProperties propiedades, AuditoriaService auditoria) {
        this.mongoTemplate = mongoTemplate;
        this.propiedades = propiedades;
        this.auditoria = auditoria;
    }

    public synchronized InfoRespaldo crearRespaldo() {
        Path carpeta = carpeta();
        String nombre = "almora-" + LocalDateTime.now(ZoneId.of("America/Bogota")).format(NOMBRE) + ".zip";
        Path destino = carpeta.resolve(nombre);
        try {
            Files.createDirectories(carpeta);
            try (OutputStream os = Files.newOutputStream(destino); ZipOutputStream zip = new ZipOutputStream(os)) {
                for (String coleccion : mongoTemplate.getCollectionNames()) {
                    if (EXCLUIDAS.contains(coleccion) || coleccion.startsWith("system.")) {
                        continue;
                    }
                    zip.putNextEntry(new ZipEntry(coleccion + ".json"));
                    for (Document doc : mongoTemplate.getCollection(coleccion).find()) {
                        zip.write(doc.toJson().getBytes(StandardCharsets.UTF_8));
                        zip.write('\n');
                    }
                    zip.closeEntry();
                }
            }
            limpiarAntiguos(carpeta);
            long bytes = Files.size(destino);
            auditoria.registrar(AccionAuditoria.PROCESO_AUTOMATICO, "Respaldo", nombre, bytes + " bytes");
            log.info("Respaldo creado: {} ({} bytes)", destino.toAbsolutePath(), bytes);
            return new InfoRespaldo(nombre, bytes, LocalDateTime.now(ZoneId.of("America/Bogota")).toString());
        } catch (IOException e) {
            log.error("No se pudo crear el respaldo", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el respaldo");
        }
    }

    public List<InfoRespaldo> listar() {
        Path carpeta = carpeta();
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .map(p -> {
                        try {
                            return new InfoRespaldo(p.getFileName().toString(), Files.size(p),
                                    Files.getLastModifiedTime(p).toInstant().toString());
                        } catch (IOException e) {
                            return new InfoRespaldo(p.getFileName().toString(), -1, null);
                        }
                    }).toList();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer la carpeta de respaldos");
        }
    }

    private void limpiarAntiguos(Path carpeta) throws IOException {
        int conservar = Math.max(1, propiedades.respaldo().conservar());
        try (Stream<Path> archivos = Files.list(carpeta)) {
            List<Path> zips = archivos.filter(p -> p.getFileName().toString().startsWith("almora-")
                            && p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
            for (int i = conservar; i < zips.size(); i++) {
                Files.deleteIfExists(zips.get(i));
            }
        }
    }

    private Path carpeta() {
        return Paths.get(propiedades.respaldo().carpeta());
    }
}
