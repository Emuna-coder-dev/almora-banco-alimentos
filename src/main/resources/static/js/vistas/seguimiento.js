// Seguimiento de una donacion tipo guia de mensajeria (los tres roles; el donante solo ve las suyas).
import { api, esAdministrativo } from "../api.js";
import { $, error, esc, fecha, fechaHora, insigniaDonacion, insigniaEntrega, num } from "../ui.js";

export async function render(c, params) {
  const codigo = params.get("codigo") || "";
  c.innerHTML = `
    <div class="encabezado-pagina">
      <div><h1>Seguimiento de donación</h1><p>Rastrea la ruta de una donación desde que se registra hasta que llega a la fundación.</p></div>
    </div>
    <div class="tarjeta" style="margin-bottom:18px">
      <form id="buscar" class="filtros" style="margin:0">
        <label style="flex:1">Código de seguimiento<input id="codigo" placeholder="Ej. DON-261014-K7P2QX" value="${esc(codigo)}"></label>
        <button class="boton" type="submit">Rastrear</button>
      </form>
      ${esAdministrativo() ? "" : `<div id="mis-codigos" style="margin-top:10px;font-size:13px;color:var(--texto-2)"></div>`}
    </div>
    <div id="resultado"></div>`;

  $("#buscar").addEventListener("submit", (e) => {
    e.preventDefault();
    const v = $("#codigo").value.trim().toUpperCase();
    if (v) location.hash = `#/seguimiento?codigo=${encodeURIComponent(v)}`;
  });

  if (!esAdministrativo()) {
    const mias = await api("/api/donaciones", { parametros: { tamano: 8 } });
    if (mias.contenido.length) {
      $("#mis-codigos").innerHTML = "Tus donaciones: " + mias.contenido.map((d) =>
        `<a href="#/seguimiento?codigo=${encodeURIComponent(d.codigo)}">${esc(d.codigo)}</a>`).join(" · ");
    }
  }
  if (codigo) await mostrar(codigo);
}

async function mostrar(codigo) {
  const r = $("#resultado");
  try {
    const s = await api(`/api/donaciones/codigo/${encodeURIComponent(codigo)}/seguimiento`);
    const p = s.progreso;
    r.innerHTML = `
      <section class="tarjeta" style="margin-bottom:18px">
        <div class="tarjeta-titulo">
          <div><h2>Guía ${esc(s.codigo)}</h2><div style="color:var(--texto-2);font-size:13px;margin-top:4px">Donante: ${esc(s.donanteNombre)}</div></div>
          ${insigniaDonacion(s.estadoActual)}
        </div>
        <p style="margin:0;color:var(--texto-2)">${esc(s.estadoDescripcion)}</p>
        <div class="ruta">${s.ruta.map((paso, i) => `
          <div class="paso ${paso.completado ? "completado" : ""} ${paso.actual ? "actual" : ""} ${["VENCIDA", "ANULADA"].includes(paso.estado) ? "alterno" : ""}">
            <div class="punto">${paso.completado ? "✓" : i + 1}</div>
            <div class="nombre">${esc(paso.titulo)}</div>
            <div class="fecha">${paso.fecha ? fechaHora(paso.fecha) : ""}</div>
          </div>`).join("")}
        </div>
      </section>

      <div class="fila-2">
        <section class="tarjeta">
          <div class="tarjeta-titulo"><h2>Historial de la ruta</h2></div>
          <ul class="eventos">${[...s.eventos].reverse().map((ev) => `<li>
            <b>${esc(ev.titulo)}</b> ${ev.referencia ? `<span class="insignia ins-gris" style="padding:2px 8px">${esc(ev.referencia)}</span>` : ""}
            <div>${esc(ev.descripcion || "")}</div>
            <small>${fechaHora(ev.fecha)}${ev.ubicacion ? " · " + esc(ev.ubicacion) : ""}${ev.responsable ? " · " + esc(ev.responsable) : ""}</small>
          </li>`).join("")}</ul>
        </section>

        <div>
          <section class="tarjeta" style="margin-bottom:18px">
            <div class="tarjeta-titulo"><h2>¿Dónde están las unidades?</h2><b>${p.porcentajeEntregado}% entregado</b></div>
            <div class="barra-progreso" role="progressbar" aria-valuenow="${p.porcentajeEntregado}" aria-valuemin="0" aria-valuemax="100"><span style="width:${p.porcentajeEntregado}%"></span></div>
            <div class="rejilla-3" style="margin-top:14px">
              <div class="dato"><small>Total</small><b>${num(p.total)}</b></div>
              <div class="dato"><small>En bodega</small><b>${num(p.enBodega)}</b></div>
              <div class="dato"><small>Asignadas</small><b>${num(p.asignadas)}</b></div>
              <div class="dato"><small>En ruta</small><b>${num(p.enRuta)}</b></div>
              <div class="dato"><small>Entregadas</small><b>${num(p.entregadas)}</b></div>
              <div class="dato"><small>Vencidas / anuladas</small><b>${num(p.vencidas + p.anuladas)}</b></div>
            </div>
          </section>
          <section class="tarjeta">
            <div class="tarjeta-titulo"><h2>Destinos</h2></div>
            ${s.destinos.length ? `<div class="tabla-envoltura"><table class="tabla">
              <thead><tr><th>Fundación</th><th>Unidades</th><th>Fecha</th><th>Estado</th></tr></thead>
              <tbody>${s.destinos.map((d) => `<tr>
                <td><b>${esc(d.fundacion)}</b><br><small style="color:var(--texto-3)">${esc(d.direccion || "")}${d.ciudad ? ", " + esc(d.ciudad) : ""} · ${esc(d.codigoEntrega)}</small>
                  ${d.receptor ? `<br><small>Recibió: ${esc(d.receptor)}</small>` : ""}</td>
                <td>${num(d.unidades)}</td>
                <td>${fecha(d.fechaEntrega || d.fechaProgramada)}</td>
                <td>${insigniaEntrega(d.estado)}</td></tr>`).join("")}</tbody></table></div>`
              : `<p style="color:var(--texto-3);margin:0">Todavía no se ha asignado a ninguna fundación. Cuando salga de bodega aparecerá aquí.</p>`}
          </section>
        </div>
      </div>`;
  } catch (e) {
    r.innerHTML = `<div class="alerta">${esc(e.message)}</div>`;
    error(e);
  }
}
