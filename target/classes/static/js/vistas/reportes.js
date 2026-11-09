// Reportes: indicadores, generacion de reportes por fechas/donante y exportacion a PDF y Excel (HU11, HU12).
import { api, descargar } from "../api.js";
import { $, aviso, error, esc, fecha, fechaHora, num, paginacion, primerDiaMes, hoyIso } from "../ui.js";

let pagina = 0;

export async function render(c) {
  pagina = 0;
  const ind = await api("/api/reportes/indicadores");
  c.innerHTML = `
    <div class="encabezado-pagina"><div><h1>Reportes</h1><p>Indicadores del banco y reportes históricos para compartir con la institución.</p></div></div>

    <div class="rejilla-4" style="margin-bottom:18px">
      <div class="dato"><small>Entregado antes de vencer</small><b style="color:var(--verde-500)">${ind.porcentajeEntregadoAntesDeVencer}%</b></div>
      <div class="dato"><small>Unidades recibidas</small><b>${num(ind.unidadesRecibidas)}</b></div>
      <div class="dato"><small>Unidades entregadas</small><b>${num(ind.unidadesEntregadas)}</b></div>
      <div class="dato"><small>Unidades vencidas</small><b style="color:var(--rojo)">${num(ind.unidadesVencidas)}</b></div>
      <div class="dato"><small>Donantes activos</small><b>${num(ind.donantesActivos)}</b></div>
      <div class="dato"><small>Fundaciones atendidas</small><b>${num(ind.fundacionesAtendidas)}</b></div>
      <div class="dato"><small>Entregas realizadas</small><b>${num(ind.entregasRealizadas)}</b></div>
      <div class="dato"><small>Correos de confirmación fallidos</small><b>${num(ind.confirmacionesFallidas)}</b></div>
    </div>

    <div class="fila-2">
      <section class="tarjeta">
        <div class="tarjeta-titulo"><h2>Generar reporte de donaciones</h2></div>
        <div class="fila-campos">
          <div class="campo"><label>Desde *</label><input type="date" id="desde" value="${primerDiaMes()}"></div>
          <div class="campo"><label>Hasta *</label><input type="date" id="hasta" value="${hoyIso()}"></div>
        </div>
        <div class="campo"><label>Donante (opcional)</label>
          <div style="display:flex;gap:8px"><input id="buscar-donante" placeholder="Nombre, NIT o correo" style="flex:1"><button class="boton secundario" id="btn-buscar">Buscar</button></div>
          <select id="donante" style="margin-top:6px"><option value="">Todos los donantes</option></select></div>
        <button class="boton" id="generar">Generar y guardar</button>
      </section>
      <section class="tarjeta">
        <div class="tarjeta-titulo"><h2>Donaciones por estado</h2></div>
        <div class="tabla-envoltura"><table class="tabla"><thead><tr><th>Estado</th><th>Donaciones</th></tr></thead>
          <tbody>${Object.entries(ind.donacionesPorEstado).filter(([, v]) => v > 0).map(([k, v]) =>
            `<tr><td>${esc(k.replace(/_/g, " ").toLowerCase().replace(/^./, (x) => x.toUpperCase()))}</td><td>${num(v)}</td></tr>`).join("")
            || `<tr><td colspan="2" class="vacio">Sin donaciones</td></tr>`}</tbody></table></div>
      </section>
    </div>

    <section class="tarjeta" style="margin-top:18px">
      <div class="tarjeta-titulo"><h2>Reportes guardados</h2></div>
      <div class="tabla-envoltura"><table class="tabla">
        <thead><tr><th>Generado</th><th>Periodo</th><th>Donante</th><th>Donaciones</th><th>Unidades</th><th>Por</th><th>Exportar</th></tr></thead>
        <tbody id="filas"></tbody></table></div>
      <div id="paginas"></div>
    </section>`;

  const buscar = async () => {
    const r = await api("/api/usuarios/donantes", { parametros: { q: $("#buscar-donante").value.trim(), tamano: 50 } });
    $("#donante").innerHTML = `<option value="">Todos los donantes</option>` +
      r.contenido.map((d) => `<option value="${esc(d.id)}">${esc(d.nombre)}</option>`).join("");
  };
  $("#btn-buscar").addEventListener("click", () => buscar().catch(error));
  $("#generar").addEventListener("click", async () => {
    try {
      const r = await api("/api/reportes", {
        metodo: "POST", cuerpo: { desde: $("#desde").value, hasta: $("#hasta").value, donanteId: $("#donante").value || null }
      });
      aviso(`Reporte generado: ${num(r.totalDonaciones)} donaciones, ${num(r.totalUnidades)} unidades`, "exito");
      pagina = 0;
      await cargar(c);
    } catch (e) {
      error(e);
    }
  });
  $("#filas").addEventListener("click", async (e) => {
    const b = e.target.closest("button[data-formato]");
    if (!b) return;
    try {
      const ext = b.dataset.formato === "pdf" ? "pdf" : "xlsx";
      await descargar(`/api/reportes/${b.dataset.id}/${b.dataset.formato}`, `reporte.${ext}`);
    } catch (err) {
      error(err);
    }
  });
  await cargar(c);
}

async function cargar(c) {
  const p = await api("/api/reportes", { parametros: { pagina, tamano: 10 } });
  $("#filas", c).innerHTML = p.contenido.length ? p.contenido.map((r) => `<tr>
    <td>${fechaHora(r.fechaGeneracion)}</td><td>${fecha(r.desde)} – ${fecha(r.hasta)}</td>
    <td>${esc(r.donanteNombre || "Todos")}</td><td>${num(r.totalDonaciones)}</td><td>${num(r.totalUnidades)}</td>
    <td><small>${esc(r.generadoPor)}</small></td>
    <td><div class="acciones"><button class="boton secundario pequeno" data-formato="pdf" data-id="${esc(r.id)}">PDF</button>
      <button class="boton secundario pequeno" data-formato="excel" data-id="${esc(r.id)}">Excel</button></div></td></tr>`).join("")
    : `<tr><td colspan="7" class="vacio">Aún no hay reportes guardados.</td></tr>`;
  $("#paginas", c).replaceChildren(paginacion(p, (n) => { pagina = n; cargar(c).catch(error); }));
}
