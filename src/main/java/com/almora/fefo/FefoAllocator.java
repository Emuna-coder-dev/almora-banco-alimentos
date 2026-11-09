package com.almora.fefo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Algoritmo FEFO (First Expired, First Out): "primero sale lo que vence primero".
 *
 * Es Java puro (no depende de Spring ni de MongoDB) para poder probarlo de forma aislada.
 * Implementa los 7 pasos del documento SCAMPER:
 *
 *  1. Recibir el producto y la cantidad solicitada.
 *  2. Tomar los lotes de ese producto con cantidad mayor que cero (y que no esten vencidos).
 *  3. Ordenarlos por fecha de vencimiento, del mas proximo al mas lejano
 *     -> se usa una COLA DE PRIORIDAD (PriorityQueue) cuya prioridad es la fecha.
 *  4. Recorrerlos en ese orden tomando de cada uno min(cantidad del lote, cantidad que falta).
 *  5. Registrar cada toma como una asignacion (lote, cantidad) y restar lo tomado.
 *  6. Detenerse cuando lo pendiente llegue a cero o se acaben los lotes.
 *  7. Si se acabaron los lotes y aun falta, informar cuanto no se pudo cubrir.
 *
 * Desempates: si dos lotes vencen el mismo dia, sale primero el que ingreso antes (FIFO);
 * los lotes sin fecha de vencimiento van al final.
 *
 * Complejidad: O(n log n) por las n inserciones/extracciones en la cola de prioridad.
 */
public final class FefoAllocator {

    public static final Comparator<LoteCandidato> ORDEN_FEFO = Comparator
            .comparing(LoteCandidato::fechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(LoteCandidato::fechaIngreso, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(LoteCandidato::loteId, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * @param cantidadSolicitada unidades que se necesitan (paso 1)
     * @param lotes              lotes del producto
     * @param hoy                fecha actual; los lotes que vencieron antes de hoy no se entregan
     */
    public ResultadoFefo asignar(int cantidadSolicitada, Collection<LoteCandidato> lotes, LocalDate hoy) {
        if (cantidadSolicitada <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que cero");
        }
        if (hoy == null) {
            throw new IllegalArgumentException("La fecha actual es obligatoria");
        }

        // Pasos 2 y 3: filtrar y meter en la cola de prioridad ordenada por vencimiento
        PriorityQueue<LoteCandidato> cola = new PriorityQueue<>(ORDEN_FEFO);
        if (lotes != null) {
            for (LoteCandidato lote : lotes) {
                boolean tieneStock = lote.cantidadDisponible() > 0;
                boolean noVencido = lote.fechaVencimiento() == null || !lote.fechaVencimiento().isBefore(hoy);
                if (tieneStock && noVencido) {
                    cola.add(lote);
                }
            }
        }

        // Pasos 4, 5 y 6
        List<AsignacionLote> asignaciones = new ArrayList<>();
        int pendiente = cantidadSolicitada;
        while (pendiente > 0 && !cola.isEmpty()) {
            LoteCandidato lote = cola.poll();               // el que vence primero
            int tomar = Math.min(lote.cantidadDisponible(), pendiente);
            asignaciones.add(new AsignacionLote(lote.loteId(), tomar));
            pendiente -= tomar;
        }

        // Paso 7: pendiente > 0 indica lo que no se pudo cubrir
        return new ResultadoFefo(cantidadSolicitada, List.copyOf(asignaciones), pendiente);
    }
}
