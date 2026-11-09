// Distribucion: entregas a fundaciones con asignacion FEFO (primero se ve la propuesta, luego se confirma).
import { api } from "../api.js";
import {
  $, $$, aviso, confirmar, error, esc, ESTADOS_ENTREGA, fecha, hoyIso, insigniaEntrega, insigniaSemaforo, modal,
  num, opciones, paginacion, pedirTexto, unidad
} from "../ui.js";

let pagina = 0;
let filtros = {};

export async function render(c) {
  pagina = 0;
  filtros = {};
  c.innerHTML = `
    <div class="encabezado-pagina">
      <div><h1>Distribución</h1><p>Entregas a fundaciones. El sistema elige los lotes que vencen primero (FEFO).</p></div>
      <button class="boton" id="nueva">+ Nueva entrega</button>
    </div>
    <div class="tarjeta">
      <div class="filtros">
        <label>Estado<select id="f-estado">${opciones(ESTADOS_ENTREGA, "", "Todos")}</select></label>
        <label>Desde<input type="date" id="f-desde"></label>
        <label>Hasta<input type="date" id="f-hasta"></label>
        <button class="boton secundario" id="filtrar">Filtrar</button>
      </div>
      <div class="tabla-envoltura"><table class="tabla">
        <thead><tr><th>Código</th><th>Fecha</th><th>Organización</th><th>Productos</th><th>Faltantes</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody id="filas"></tbody></table></div>
      <div id="paginas"></div>
    </div>`;
  $("#nueva").addEventListener("click", () => nuevaEntrega(c).catch(error));
  $("#filtrar").addEventListener("click", () => {
    filtros = { estado: $("#f-estado").value, desde: $("#f-desde").value, hasta: $("#f-hasta").value };
    pagina = 0;
    cargar(c).catch(error);
  });
  $("#filas").addEventListener("click", (e) => accion(e, c));
  await cargar(c);
}

async function cargar(c) {
  const p = await api("/api/entregas", { parametros: { ...filtros, pagina, tamano: 15 } });
  $("#filas", c).innerHTML = p.contenido.length ? p.contenido.map((e) => {
    const productos = resumir(e.items);
    const b = (a, t, cl = "secundario") => `<button class="boton ${cl} pequeno" data-accion="${a}" data-id="${esc(e.id)}" data-codigo="${esc(e.codigo)}">${t}</button>`;
    let acciones = b("ver", "Ver ruta");
    if (e.estado === "PROGRAMADA") acciones += b("ruta", "Despachar", "");
    if (e.estado === "PROGRAMADA" || e.estado === "EN_RUTA") acciones += b("entregar", "Marcar entregada", "") + b("cancelar", "Cancelar", "peligro");
    return `<tr>
      <td><b>${esc(e.codigo)}</b></td>
      <td>${fecha(e.fechaEntrega || e.fechaProgramada)}</td>
      <td>${esc(e.fundacionNombre)}<br><small style="color:var(--texto-3)">${esc(e.ciudadDestino || "")}</small></td>
      <td style="max-width:280px">${productos}</td>
      <td>${e.faltantes && e.faltantes.length ? e.faltantes.map((f) => `${esc(f.nombreProducto)}: faltan ${num(f.noCubierto)}`).join("<br>") : "-"}</td>
      <td>${insigniaEntrega(e.estado)}</td>
      <td><div class="acciones">${acciones}</div></td></tr>`;
  }).join("") : `<tr><td colspan="7" class="vacio">No hay entregas.</td></tr>`;
  $("#paginas", c).replaceChildren(paginacion(p, (n) => { pagina = n; cargar(c).catch(error); }));
}

function resumir(items) {
  const porProducto = {};
  items.forEach((i) => {
    const k = i.nombreProducto + "|" + i.unidadMedida;
    porProducto[k] = (porProducto[k] || 0) + i.cantidad;
  });
  return Object.entries(porProducto).map(([k, v]) => {
    const [nombre, u] = k.split("|");
    return `${esc(nombre)} (${num(v)} ${unidad(u)})`;
  }).join(", ");
}

async function accion(ev, c) {
  const b = ev.target.closest("button[data-accion]");
  if (!b) return;
  const { accion: a, id, codigo } = b.dataset;
  try {
    if (a === "ver") {
      const e = await api(`/api/entregas/${id}`);
      modal(`Ruta de la entrega ${e.codigo}`, `
        <p><b>${esc(e.fundacionNombre)}</b> · ${esc(e.direccionDestino || "")}, ${esc(e.ciudadDestino || "")}</p>
        <div class="tabla-envoltura" style="margin-bottom:14px"><table class="tabla">
          <thead><tr><th>Lote</th><th>Producto</th><th>Cantidad</th><th>Vence</th><th>Donación</th></tr></thead>
          <tbody>${e.items.map((i) => `<tr><td>${esc(i.codigoLote)}</td><td>${esc(i.nombreProducto)}</td>
            <td>${num(i.cantidad)} ${unidad(i.unidadMedida)}</td><td>${i.fechaVencimiento ? fecha(i.fechaVencimiento) : "No vence"}</td>
            <td>${esc(i.codigoDonacion)}</td></tr>`).join("")}</tbody></table></div>
        <ul class="eventos">${[...e.seguimiento].reverse().map((s) => `<li><b>${esc(s.titulo)}</b><div>${esc(s.descripcion || "")}</div>
          <small>${new Date(s.fecha).toLocaleString("es-CO")}${s.ubicacion ? " · " + esc(s.ubicacion) : ""} · ${esc(s.responsable || "")}</small></li>`).join("")}</ul>`,
        [{ texto: "Cerrar", clase: "secundario" }], { ancho: true });
      return;
    }
    if (a === "ruta") {
      const ubic = await pedirTexto(`Despachar ${codigo}`, "¿Dónde va? (ubicación actual, opcional)", { valor: "Saliendo de bodega" });
      if (ubic === null) return;
      await api(`/api/entregas/${id}/estado`, { metodo: "PATCH", cuerpo: { estado: "EN_RUTA", ubicacion: ubic } });
      aviso(`${codigo} en ruta`, "exito");
    } else if (a === "entregar") {
      const receptor = await pedirTexto(`Entregar ${codigo}`, "Nombre de quien recibe en la fundación");
      if (receptor === null) return;
      await api(`/api/entregas/${id}/estado`, { metodo: "PATCH", cuerpo: { estado: "ENTREGADA", receptor } });
      aviso(`${codigo} entregada`, "exito");
    } else if (a === "cancelar") {
      const motivo = await pedirTexto(`Cancelar ${codigo}`, "Motivo de la cancelación", { minimo: 3 });
      if (motivo === null) return;
      await api(`/api/entregas/${id}/estado`, { metodo: "PATCH", cuerpo: { estado: "CANCELADA", observacion: motivo } });
      aviso(`${codigo} cancelada: los productos volvieron a bodega`, "exito");
    }
    await cargar(c);
  } catch (err) {
    error(err);
  }
}

// ------------------------------------------------------------------ nueva entrega con FEFO
async function nuevaEntrega(c) {
  const [fundaciones, inventario] = await Promise.all([
    api("/api/fundaciones", { parametros: { activa: true } }),
    api("/api/inventario")
  ]);
  if (!fundaciones.length) {
    aviso("Primero registra una fundación en Organizaciones.", "error");
    return;
  }
  const conStock = inventario.filter((p) => p.stockDisponible > 0);
  if (!conStock.length) {
    aviso("No hay productos disponibles en el inventario.", "error");
    return;
  }
  const opcionesProducto = conStock.map((p) =>
    `<option value="${esc(p.productoId)}">${esc(p.nombre)} · ${num(p.stockDisponible)} ${unidad(p.unidadMedida)} disponibles</option>`).join("");
  const fila = () => `<div class="item-fila" style="grid-template-columns:2fr 1fr auto">
    <select data-campo="productoId" aria-label="Producto">${opcionesProducto}</select>
    <input type="number" min="1" data-campo="cantidad" placeholder="Cantidad" aria-label="Cantidad">
    <button type="button" class="boton peligro pequeno" data-quitar aria-label="Quitar">✕</button></div>`;

  const leer = (w) => ({
    fundacionId: $("#fundacion", w).value,
    fechaProgramada: $("#fecha-prog", w).value || null,
    observaciones: $("#obs", w).value.trim() || null,
    permitirParcial: $("#parcial", w).checked,
    items: $$(".item-fila", w).map((f) => ({
      productoId: $("[data-campo=productoId]", f).value,
      cantidad: Number($("[data-campo=cantidad]", f).value)
    }))
  });

  const v = modal("Nueva entrega", `
    <div class="fila-campos">
      <div class="campo"><label>Fundación *</label><select id="fundacion">${fundaciones.map((f) =>
        `<option value="${esc(f.id)}">${esc(f.nombre)} · ${esc(f.ciudad)}</option>`).join("")}</select></div>
      <div class="campo"><label>Fecha programada</label><input type="date" id="fecha-prog" min="${hoyIso()}" value="${hoyIso()}"></div>
    </div>
    <div class="campo"><label>Productos que necesita la fundación *</label></div>
    <div class="items-formulario" id="items">${fila()}</div>
    <button type="button" class="boton secundario pequeno" id="agregar">+ Agregar producto</button>
    <div class="campo" style="margin-top:12px"><label>Observaciones</label><textarea id="obs"></textarea></div>
    <label style="display:flex;gap:8px;align-items:center;font-size:13px"><input type="checkbox" id="parcial" checked> Permitir entrega parcial si no alcanza el inventario</label>
    <div id="propuesta" style="margin-top:16px"></div>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Ver propuesta FEFO", clase: "secundario", accion: async (w) => {
        const d = leer(w);
        if (d.items.some((i) => !i.cantidad || i.cantidad < 1)) {
          aviso("Escribe una cantidad en cada producto", "error");
          return false;
        }
        const p = await api("/api/entregas/previsualizar", { metodo: "POST", cuerpo: d });
        $("#propuesta", w).innerHTML = `
          <div class="alerta ${p.completa ? "info" : ""}">${p.completa ? "El inventario alcanza para todo el pedido." : "El inventario no alcanza para todo: revisa los faltantes."}
            Estos son los lotes que saldrían, del que vence primero al último:</div>
          ${p.productos.map((pr) => `<p style="margin:10px 0 6px"><b>${esc(pr.nombreProducto)}</b>: ${num(pr.cubierto)} de ${num(pr.solicitado)}
            ${pr.noCubierto ? `<span class="insignia ins-rojo">faltan ${num(pr.noCubierto)}</span>` : ""}</p>
            <div class="tabla-envoltura"><table class="tabla"><thead><tr><th>Lote</th><th>Toma</th><th>Vence</th><th>Días</th><th>Semáforo</th><th>Donación</th></tr></thead>
            <tbody>${pr.lotes.map((l) => `<tr><td>${esc(l.codigoLote)}</td><td>${num(l.cantidad)}</td>
              <td>${l.fechaVencimiento ? fecha(l.fechaVencimiento) : "No vence"}</td><td>${l.diasParaVencer ?? "-"}</td>
              <td>${insigniaSemaforo(l.semaforo)}</td><td>${esc(l.codigoDonacion)}</td></tr>`).join("") || `<tr><td colspan="6" class="vacio">Sin lotes disponibles</td></tr>`}</tbody></table></div>`).join("")}`;
        return false;
      }
    },
    {
      texto: "Confirmar entrega", accion: async (w) => {
        const d = leer(w);
        if (d.items.some((i) => !i.cantidad || i.cantidad < 1)) {
          aviso("Escribe una cantidad en cada producto", "error");
          return false;
        }
        if (!$("#propuesta", w).innerHTML.trim() &&
          !(await confirmar("Confirmar sin revisar", "No has visto la propuesta FEFO. ¿Crear la entrega de todas formas?"))) {
          return false;
        }
        const e = await api("/api/entregas", { metodo: "POST", cuerpo: d });
        aviso(`Entrega ${e.codigo} programada con ${num(e.totalUnidades)} unidades`, "exito");
        await cargar(c);
      }
    }
  ], { ancho: true });
  $("#agregar", v).addEventListener("click", () => $("#items", v).insertAdjacentHTML("beforeend", fila()));
  $("#items", v).addEventListener("click", (e) => {
    if (e.target.closest("[data-quitar]") && $$(".item-fila", v).length > 1) e.target.closest(".item-fila").remove();
  });
  $("#items", v).addEventListener("input", () => { $("#propuesta", v).innerHTML = ""; });
}
