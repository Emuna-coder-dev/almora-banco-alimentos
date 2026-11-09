package com.almora.scheduler;

import com.almora.config.AlmoraProperties;
import com.almora.model.auditoria.AccionAuditoria;
import com.almora.model.inventario.Lote;
import com.almora.service.AuditoriaService;
import com.almora.service.DonacionService;
import com.almora.service.InventarioService;
import com.almora.service.RespaldoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tareas automaticas:
 *  - Todos los dias (y al arrancar): marca como VENCIDOS los lotes cuya fecha ya paso (HU10) y
 *    actualiza el seguimiento de las donaciones afectadas.
 *  - Cada semana: copia de seguridad de la base de datos (RNF-BD01).
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final InventarioService inventario;
    private final DonacionService donacionService;
    private final RespaldoService respaldoService;
    private final AuditoriaService auditoria;
    private final AlmoraProperties propiedades;

    public TareasProgramadas(InventarioService inventario, DonacionService donacionService,
                             RespaldoService respaldoService, AuditoriaService auditoria, AlmoraProperties propiedades) {
        this.inventario = inventario;
        this.donacionService = donacionService;
        this.respaldoService = respaldoService;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        marcarVencidos();
    }

    @Scheduled(cron = "${almora.fefo.cron-vencimientos:0 5 0 * * *}", zone = "America/Bogota")
    public void marcarVencidos() {
        try {
            List<Lote> vencidos = inventario.marcarVencidos();
            if (vencidos.isEmpty()) {
                return;
            }
            Set<String> donaciones = new LinkedHashSet<>();
            long unidades = 0;
            for (Lote l : vencidos) {
                donaciones.add(l.getDonacionId());
                unidades += l.getCantidadVencida();
            }
            donaciones.forEach(donacionService::recalcularEstado);
            auditoria.registrar(AccionAuditoria.PROCESO_AUTOMATICO, "Lote", null,
                    vencidos.size() + " lote(s) marcados como vencidos (" + unidades + " unidades)");
            log.info("Vencimientos: {} lote(s) marcados como VENCIDO", vencidos.size());
        } catch (RuntimeException e) {
            log.error("Fallo la tarea de vencimientos", e);
        }
    }

    @Scheduled(cron = "${almora.respaldo.cron:0 0 2 * * SUN}", zone = "America/Bogota")
    public void respaldoSemanal() {
        if (propiedades.respaldo() == null || !propiedades.respaldo().habilitado()) {
            return;
        }
        try {
            respaldoService.crearRespaldo();
        } catch (RuntimeException e) {
            log.error("Fallo el respaldo programado", e);
        }
    }
}
