// Donaciones: listado, registro con varios productos y acciones segun el rol.
import { api, descargar, esAdministrativo } from "../api.js";
import {
  $, $$, aviso, confirmar, error, esc, ESTADOS_DONACION, fecha, hoyIso, insigniaDonacion, modal, num, opciones,
  paginacion, pedirTexto, unidad
} from "../ui.js";

let estado = { pagina: 0, filtros: {} };

export async function render(c, params) {
  const admin = esAdministrativo();
  estado = { pagina: 0, filtros: {} };
  c.innerHTML = `
    <div class="encabezado-pagina">
      <div><h1>${admin ? "Donaciones" : "Mis donaciones"}</h1>
        <p>${admin ? "Registra lo que llega, cambia su estado y consulta su ruta." : "Registra tus donaciones, sigue su ruta y descarga tus certificados."}</p></div>
      <button class="boton" id="nueva">+ Registrar donación</button>
    </div>
    <div class="tarjeta">
      <div class="filtros">
        <label>Estado<select id="f-estado">${opciones(ESTADOS_DONACION, "", "Todos")}</select></label>
        <label>Desde<input type="date" id="f-desde"></label>
        <label>Hasta<input type="date" id="f-hasta"></label>
        <label>Código<input id="f-codigo" placeholder="DON-..."></label>
        ${admin ? `<label>Confirmación<select id="f-confirmacion"><option value="">Todas</option>
          <option value="FALLIDA">Fallidas</option><option value="ENVIADA">Enviadas</option><option value="DESHABILITADA">Deshabilitadas</option></select></label>` : ""}
        <button class="boton secundario" id="filtrar">Filtrar</button>
      </div>
      <div class="tabla-envoltura"><table class="tabla">
        <thead><tr><th>Código</th><th>Fecha</th>${admin ? "<th>Donante</th>" : ""}<th>Productos</th><th>Unidades</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody id="filas"></tbody>
      </table></div>
      <div id="paginas"></div>
    </div>`;

  $("#nueva").addEventListener("click", () => formularioDonacion(c));
  $("#filtrar").addEventListener("click", () => {
    estado.filtros = {
      estado: $("#f-estado").value, desde: $("#f-desde").value, hasta: $("#f-hasta").value,
      codigo: $("#f-codigo").value.trim(), confirmacion: admin ? $("#f-confirmacion").value : ""
    };
    estado.pagina = 0;
    cargar(c);
  });
  $("#filas").addEventListener("click", (e) => accion(e, c));
  await cargar(c);
  if (params.get("nueva")) formularioDonacion(c);
}

async function cargar(c) {
  const admin = esAdministrativo();
  const p = await api("/api/donaciones", { parametros: { ...estado.filtros, pagina: estado.pagina, tamano: 15 } });
  const columnas = admin ? 7 : 6;
  $("#filas", c).innerHTML = p.contenido.length ? p.contenido.map((d) => {
    const productos = d.items.map((i) => `${esc(i.nombreProducto)} (${num(i.cantidad)} ${unidad(i.unidadMedida)})`).join(", ");
    const conf = admin && d.confirmacion && d.confirmacion.estado === "FALLIDA"
      ? `<br><small style="color:var(--rojo)">Correo no enviado</small>` : "";
    return `<tr>
      <td><b>${esc(d.codigo)}</b></td>
      <td>${fecha(d.fechaRegistro)}</td>
      ${admin ? `<td>${esc(d.donanteNombre)}</td>` : ""}
      <td style="max-width:300px">${productos}</td>
      <td>${num(d.totalUnidades)}</td>
      <td>${insigniaDonacion(d.estado)}${conf}</td>
      <td><div class="acciones">${botones(d, admin)}</div></td>
    </tr>`;
  }).join("") : `<tr><td colspan="${columnas}" class="vacio">No hay donaciones con estos filtros.</td></tr>`;
  $("#paginas", c).replaceChildren(paginacion(p, (n) => { estado.pagina = n; cargar(c); }));
}

function botones(d, admin) {
  const b = (accion, texto, clase = "secundario") =>
    `<button class="boton ${clase} pequeno" data-accion="${accion}" data-id="${esc(d.id)}" data-codigo="${esc(d.codigo)}">${texto}</button>`;
  let html = b("ruta", "Ver ruta");
  if (d.certificadoDisponible) html += b("certificado", "Certificado");
  if (admin && d.estado === "PENDIENTE") html += b("recibir", "Recibir", "");
  if (admin && d.estado === "RECIBIDA") html += b("almacenar", "Almacenar", "");
  if (admin && d.confirmacion && d.confirmacion.estado === "FALLIDA") html += b("reenviar", "Reenviar correo");
  if (d.estado === "PENDIENTE") html += b("editar", "Editar");
  const anulable = admin ? ["PENDIENTE", "RECIBIDA", "ALMACENADA"].includes(d.estado) : d.estado === "PENDIENTE";
  if (anulable) html += b("anular", "Anular", "peligro");
  return html;
}

async function accion(e, c) {
  const boton = e.target.closest("button[data-accion]");
  if (!boton) return;
  const { accion: a, id, codigo } = boton.dataset;
  try {
    if (a === "ruta") {
      location.hash = `#/seguimiento?codigo=${encodeURIComponent(codigo)}`;
    } else if (a === "certificado") {
      await descargar(`/api/donaciones/${id}/certificado`, `certificado-${codigo}.pdf`);
    } else if (a === "recibir") {
      const ubic = await pedirTexto("Recibir donación", "¿En qué ubicación de bodega queda? (opcional)", { valor: "Bodega principal" });
      if (ubic === null) return;
      await api(`/api/donaciones/${id}/estado`, { metodo: "PATCH", cuerpo: { estado: "RECIBIDA", ubicacionAlmacen: ubic } });
      aviso(`${codigo} recibida: ya está en el inventario`, "exito");
    } else if (a === "almacenar") {
      const ubic = await pedirTexto("Almacenar donación", "Ubicación en bodega (estante, zona…)", { valor: "Estante A1" });
      if (ubic === null) return;
      await api(`/api/donaciones/${id}/estado`, { metodo: "PATCH", cuerpo: { estado: "ALMACENADA", ubicacionAlmacen: ubic } });
      aviso(`${codigo} almacenada`, "exito");
    } else if (a === "reenviar") {
      const r = await api(`/api/donaciones/${id}/reenviar-confirmacion`, { metodo: "POST" });
      aviso(`Confirmación: ${r.estado}`, r.estado === "ENVIADA" ? "exito" : "error");
    } else if (a === "editar") {
      const d = await api(`/api/donaciones/${id}`);
      formularioDonacion(c, d);
      return;
    } else if (a === "anular") {
      const motivo = await pedirTexto(`Anular ${codigo}`, "Motivo de la anulación (queda en auditoría)", { minimo: 5 });
      if (motivo === null) return;
      if (!(await confirmar("Confirmar anulación", `La donación ${codigo} se anulará y sus productos saldrán del inventario. ¿Continuar?`, "Anular", "peligro"))) return;
      await api(`/api/donaciones/${id}/anular`, { metodo: "POST", cuerpo: { motivo } });
      aviso(`${codigo} anulada`, "exito");
    }
    await cargar(c);
  } catch (err) {
    error(err);
  }
}

// ------------------------------------------------------------------ formulario
async function formularioDonacion(c, existente = null) {
  const admin = esAdministrativo();
  const productos = await api("/api/productos");
  if (!productos.length) {
    aviso("No hay productos en el catálogo. Un administrador debe crearlos primero (Inventario → Productos).", "error");
    return;
  }
  const opcionesProducto = productos.map((p) =>
    `<option value="${esc(p.id)}" data-perecedero="${p.perecedero}">${esc(p.nombre)} (${unidad(p.unidadMedida)})${p.perecedero ? " · perecedero" : ""}</option>`).join("");

  const fila = (item = {}) => `<div class="item-fila">
      <select data-campo="productoId" aria-label="Producto">${opcionesProducto}</select>
      <input type="number" min="1" data-campo="cantidad" placeholder="Cantidad" value="${esc(item.cantidad || "")}" aria-label="Cantidad">
      <input type="date" min="${hoyIso()}" data-campo="fechaVencimiento" value="${esc(item.fechaVencimiento || "")}" aria-label="Fecha de vencimiento">
      <button type="button" class="boton peligro pequeno" data-quitar aria-label="Quitar producto">✕</button>
    </div>`;

  const contenido = `
    ${admin && !existente ? `<div class="campo"><label>Donante *</label>
      <div style="display:flex;gap:8px"><input id="buscar-donante" placeholder="Buscar por nombre, NIT o correo" style="flex:1">
      <button type="button" class="boton secundario" id="btn-buscar">Buscar</button></div>
      <select id="donante" style="margin-top:6px"><option value="">Escribe y pulsa Buscar…</option></select></div>
      <div class="campo"><label>Ubicación en bodega</label><input id="ubicacion" value="Bodega principal"></div>` : ""}
    <div class="campo"><label>Productos * <small>(la fecha de vencimiento es obligatoria si el producto es perecedero)</small></label></div>
    <div class="items-formulario" id="items">${(existente ? existente.items : [{}]).map(fila).join("")}</div>
    <button type="button" class="boton secundario pequeno" id="agregar">+ Agregar producto</button>
    <div class="campo" style="margin-top:14px"><label>Observaciones</label><textarea id="observaciones" maxlength="500">${esc(existente ? existente.observaciones : "")}</textarea></div>
    ${admin && !existente ? `<div class="alerta info">Al registrarla un administrador, la donación entra de una vez al inventario como lotes.</div>` : ""}`;

  const v = modal(existente ? `Editar ${existente.codigo}` : "Registrar donación", contenido, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: existente ? "Guardar cambios" : "Registrar", accion: async (w) => {
        const items = $$(".item-fila", w).map((f) => ({
          productoId: $("[data-campo=productoId]", f).value,
          cantidad: Number($("[data-campo=cantidad]", f).value),
          fechaVencimiento: $("[data-campo=fechaVencimiento]", f).value || null
        }));
        if (items.some((i) => !i.cantidad || i.cantidad < 1)) {
          aviso("Escribe una cantidad mayor que cero en cada producto", "error");
          return false;
        }
        const observaciones = $("#observaciones", w).value.trim() || null;
        let r;
        if (existente) {
          r = await api(`/api/donaciones/${existente.id}`, { metodo: "PUT", cuerpo: { items, observaciones } });
        } else {
          const cuerpo = { items, observaciones };
          if (admin) {
            cuerpo.donanteId = $("#donante", w).value;
            cuerpo.ubicacionAlmacen = $("#ubicacion", w).value.trim() || null;
            if (!cuerpo.donanteId) {
              aviso("Selecciona el donante", "error");
              return false;
            }
          }
          r = await api("/api/donaciones", { metodo: "POST", cuerpo });
        }
        aviso(`Donación ${r.codigo} guardada (${num(r.totalUnidades)} unidades). Estado: ${r.estadoTitulo}`, "exito");
        await cargar(c);
      }
    }
  ], { ancho: true });

  if (existente) {
    $$(".item-fila", v).forEach((f, i) => { $("[data-campo=productoId]", f).value = existente.items[i].productoId; });
  }
  $("#agregar", v).addEventListener("click", () => $("#items", v).insertAdjacentHTML("beforeend", fila()));
  $("#items", v).addEventListener("click", (e) => {
    if (e.target.closest("[data-quitar]") && $$(".item-fila", v).length > 1) e.target.closest(".item-fila").remove();
  });

  if (admin && !existente) {
    const buscar = async () => {
      try {
        const r = await api("/api/usuarios/donantes", { parametros: { q: $("#buscar-donante", v).value.trim(), tamano: 50 } });
        $("#donante", v).innerHTML = r.contenido.length
          ? r.contenido.map((d) => `<option value="${esc(d.id)}">${esc(d.nombre)} · ${esc(d.identificacion || d.correo)}</option>`).join("")
          : `<option value="">Sin resultados: registra el donante en Configuración → Donantes</option>`;
      } catch (err) {
        error(err);
      }
    };
    $("#btn-buscar", v).addEventListener("click", buscar);
    $("#buscar-donante", v).addEventListener("keydown", (e) => { if (e.key === "Enter") { e.preventDefault(); buscar(); } });
    buscar();
  }
}
