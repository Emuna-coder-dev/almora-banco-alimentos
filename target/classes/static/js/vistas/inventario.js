// Inventario FEFO: existencias por producto, lotes en orden FEFO con semaforo, alertas (HU10) y catalogo.
import { api } from "../api.js";
import {
  $, $$, aviso, categoria, CATEGORIAS, error, esc, fecha, iconoCategoria, insigniaSemaforo, leerFormulario, modal,
  num, opciones, paginacion, unidad
} from "../ui.js";

const UNIDADES = { UNIDAD: "Unidad", KILOGRAMO: "Kilogramo", LIBRA: "Libra", LITRO: "Litro", PAQUETE: "Paquete", CAJA: "Caja" };
let paginaLotes = 0;

export async function render(c, params) {
  const sem = await api("/api/inventario/semaforo");
  c.innerHTML = `
    <div class="encabezado-pagina">
      <div><h1>Inventario</h1><p>Los lotes se ordenan con FEFO: sale primero lo que vence primero.</p></div>
    </div>
    <div class="rejilla-4" style="margin-bottom:18px">
      <div class="dato"><small>Lotes en verde (más de ${sem.diasAmarillo} días)</small><b style="color:var(--verde-500)">${num(sem.verde)}</b></div>
      <div class="dato"><small>Lotes en amarillo (${sem.diasRojo + 1} a ${sem.diasAmarillo} días)</small><b style="color:#c98a06">${num(sem.amarillo)}</b></div>
      <div class="dato"><small>Lotes en rojo (${sem.diasRojo} días o menos)</small><b style="color:var(--rojo)">${num(sem.rojo)}</b></div>
      <div class="dato"><small>Lotes sin fecha de vencimiento</small><b>${num(sem.sinVencimiento)}</b></div>
    </div>
    <div class="tarjeta">
      <div class="pestanas" role="tablist">
        <button data-pestana="productos-stock">Por producto</button>
        <button data-pestana="lotes">Lotes (FEFO)</button>
        <button data-pestana="alertas">Próximos a vencer</button>
        <button data-pestana="catalogo">Catálogo de productos</button>
      </div>
      <div id="panel"></div>
    </div>`;
  const pestanas = { "productos-stock": porProducto, lotes, alertas, catalogo };
  const abrir = (id) => {
    $$("[data-pestana]", c).forEach((b) => b.classList.toggle("activa", b.dataset.pestana === id));
    pestanas[id]($("#panel", c)).catch(error);
  };
  $$("[data-pestana]", c).forEach((b) => b.addEventListener("click", () => abrir(b.dataset.pestana)));
  abrir(params.get("vista") === "alertas" ? "alertas" : "productos-stock");
}

// ------------------------------------------------------------------ por producto (HU09)
async function porProducto(panel) {
  panel.innerHTML = `
    <div class="filtros">
      <label>Tipo de producto<select id="f-cat">${opciones(CATEGORIAS, "", "Todos")}</select></label>
      <label>Vence antes de<input type="date" id="f-vence"></label>
      <label>Semáforo<select id="f-sem"><option value="">Todos</option><option value="VERDE">Verde</option>
        <option value="AMARILLO">Amarillo</option><option value="ROJO">Rojo</option></select></label>
      <button class="boton secundario" id="filtrar">Filtrar</button>
    </div>
    <div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Producto</th><th>Tipo</th><th>Disponible</th><th>Reservado</th><th>Lotes</th><th>Próximo vencimiento</th><th>Semáforo</th><th>Estado</th></tr></thead>
      <tbody id="filas"></tbody></table></div>`;
  const cargar = async () => {
    const datos = await api("/api/inventario", {
      parametros: { categoria: $("#f-cat").value, venceAntesDe: $("#f-vence").value, semaforo: $("#f-sem").value }
    });
    $("#filas").innerHTML = datos.length ? datos.map((p) => `<tr>
      <td><div class="producto-celda"><span class="producto-icono">${iconoCategoria(p.categoria)}</span><b>${esc(p.nombre)}</b></div></td>
      <td>${esc(categoria(p.categoria))}${p.perecedero ? " · perecedero" : ""}</td>
      <td>${num(p.stockDisponible)} ${unidad(p.unidadMedida)}</td>
      <td>${num(p.stockAsignado)}</td>
      <td>${num(p.lotesDisponibles)}</td>
      <td>${p.proximoVencimiento ? `${fecha(p.proximoVencimiento)} <small style="color:var(--texto-3)">(${p.diasParaVencer} días)</small>` : "-"}</td>
      <td>${p.semaforo ? insigniaSemaforo(p.semaforo) : "-"}</td>
      <td>${{ DISPONIBLE: '<span class="insignia ins-verde">Disponible</span>', POR_VENCER: '<span class="insignia ins-amarillo">Por vencer</span>', AGOTADO: '<span class="insignia ins-gris">Agotado</span>' }[p.estadoStock]}</td>
    </tr>`).join("") : `<tr><td colspan="8" class="vacio">No hay existencias con estos filtros.</td></tr>`;
  };
  $("#filtrar").addEventListener("click", () => cargar().catch(error));
  await cargar();
}

// ------------------------------------------------------------------ lotes en orden FEFO
function filasLotes(lista) {
  return lista.length ? lista.map((l) => `<tr>
    <td>${l.ordenFefo ? `<span class="insignia ins-verde" title="Posición en la cola FEFO de este producto">#${l.ordenFefo}</span>` : ""}</td>
    <td><div class="producto-celda"><span class="producto-icono">${iconoCategoria(l.categoria)}</span><div><b>${esc(l.nombreProducto)}</b><br><small style="color:var(--texto-3)">${esc(l.codigo)}</small></div></div></td>
    <td>${num(l.cantidadDisponible)} ${unidad(l.unidadMedida)}</td>
    <td>${num(l.cantidadAsignada)}</td>
    <td>${l.fechaVencimiento ? fecha(l.fechaVencimiento) : "No vence"}</td>
    <td>${l.diasParaVencer === null || l.diasParaVencer === undefined ? "-" : num(l.diasParaVencer)}</td>
    <td>${insigniaSemaforo(l.semaforo)}</td>
    <td><a href="#/seguimiento?codigo=${encodeURIComponent(l.codigoDonacion)}">${esc(l.codigoDonacion)}</a></td>
    <td>${esc(l.ubicacionAlmacen || "-")}</td>
  </tr>`).join("") : `<tr><td colspan="9" class="vacio">No hay lotes.</td></tr>`;
}
const CABECERA_LOTES = `<thead><tr><th>FEFO</th><th>Producto / lote</th><th>Disponible</th><th>Reservado</th><th>Vence</th><th>Días</th><th>Semáforo</th><th>Donación</th><th>Ubicación</th></tr></thead>`;

async function lotes(panel) {
  paginaLotes = 0;
  panel.innerHTML = `
    <div class="filtros">
      <label>Tipo de producto<select id="f-cat">${opciones(CATEGORIAS, "", "Todos")}</select></label>
      <label>Semáforo<select id="f-sem"><option value="">Todos</option><option value="VERDE">Verde</option>
        <option value="AMARILLO">Amarillo</option><option value="ROJO">Rojo</option><option value="SIN_VENCIMIENTO">Sin fecha</option></select></label>
      <label>Estado<select id="f-est"><option value="">Con existencias</option><option value="DISPONIBLE">Disponible</option>
        <option value="AGOTADO">Agotado</option><option value="VENCIDO">Vencido</option><option value="ANULADO">Anulado</option></select></label>
      <button class="boton secundario" id="filtrar">Filtrar</button>
    </div>
    <div class="tabla-envoltura"><table class="tabla">${CABECERA_LOTES}<tbody id="filas"></tbody></table></div>
    <div id="paginas"></div>`;
  const cargar = async () => {
    const p = await api("/api/inventario/lotes", {
      parametros: { categoria: $("#f-cat").value, semaforo: $("#f-sem").value, estado: $("#f-est").value, pagina: paginaLotes, tamano: 25 }
    });
    $("#filas").innerHTML = filasLotes(p.contenido);
    $("#paginas").replaceChildren(paginacion(p, (n) => { paginaLotes = n; cargar().catch(error); }));
  };
  $("#filtrar").addEventListener("click", () => { paginaLotes = 0; cargar().catch(error); });
  await cargar();
}

// ------------------------------------------------------------------ alertas (HU10)
async function alertas(panel) {
  panel.innerHTML = `
    <div class="filtros">
      <label>Vencen en los próximos<select id="f-dias"><option value="5">5 días</option><option value="7">7 días</option>
        <option value="15">15 días</option><option value="30">30 días</option></select></label>
      <button class="boton secundario" id="filtrar">Ver</button>
    </div>
    <div id="resumen"></div>
    <div class="tabla-envoltura"><table class="tabla">${CABECERA_LOTES}<tbody id="filas"></tbody></table></div>`;
  const cargar = async () => {
    const a = await api("/api/inventario/alertas", { parametros: { dias: $("#f-dias").value } });
    $("#resumen").innerHTML = a.totalLotes
      ? `<div class="alerta">${num(a.totalLotes)} lote(s) con ${num(a.totalUnidadesEnRiesgo)} unidades vencen en los próximos ${a.diasAlerta} días. Priorízalos en la próxima distribución.</div>`
      : `<div class="alerta info">No hay productos que venzan en los próximos ${a.diasAlerta} días.</div>`;
    $("#filas").innerHTML = filasLotes(a.lotes);
  };
  $("#filtrar").addEventListener("click", () => cargar().catch(error));
  await cargar();
}

// ------------------------------------------------------------------ catalogo de productos
async function catalogo(panel) {
  panel.innerHTML = `
    <div class="filtros" style="justify-content:space-between">
      <label>Buscar<input id="f-q" placeholder="Nombre del producto"></label>
      <button class="boton" id="nuevo">+ Nuevo producto</button>
    </div>
    <div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Producto</th><th>Tipo</th><th>Unidad</th><th>Perecedero</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody id="filas"></tbody></table></div>`;
  let lista = [];
  const cargar = async () => {
    lista = await api("/api/productos", { parametros: { q: $("#f-q").value.trim(), incluirInactivos: true } });
    $("#filas").innerHTML = lista.length ? lista.map((p) => `<tr>
      <td><div class="producto-celda"><span class="producto-icono">${iconoCategoria(p.categoria)}</span><b>${esc(p.nombre)}</b></div></td>
      <td>${esc(categoria(p.categoria))}</td><td>${esc(UNIDADES[p.unidadMedida])}</td><td>${p.perecedero ? "Sí" : "No"}</td>
      <td>${p.activo ? '<span class="insignia ins-verde">Activo</span>' : '<span class="insignia ins-gris">Inactivo</span>'}</td>
      <td><div class="acciones"><button class="boton secundario pequeno" data-editar="${esc(p.id)}">Editar</button>
        <button class="boton secundario pequeno" data-activar="${esc(p.id)}" data-valor="${!p.activo}">${p.activo ? "Desactivar" : "Activar"}</button></div></td>
    </tr>`).join("") : `<tr><td colspan="6" class="vacio">No hay productos.</td></tr>`;
  };
  const formulario = (p = null) => modal(p ? "Editar producto" : "Nuevo producto", `
    <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(p ? p.nombre : "")}" maxlength="100"></div>
    <div class="fila-campos">
      <div class="campo"><label>Tipo *</label><select name="categoria">${opciones(CATEGORIAS, p ? p.categoria : "GRANOS_Y_CEREALES")}</select></div>
      <div class="campo"><label>Unidad de medida *</label><select name="unidadMedida">${opciones(UNIDADES, p ? p.unidadMedida : "KILOGRAMO")}</select></div>
    </div>
    <div class="campo"><label>¿Es perecedero? (exige fecha de vencimiento al donarlo)</label>
      <select name="perecedero"><option value="true" ${p && p.perecedero ? "selected" : ""}>Sí</option><option value="false" ${p && !p.perecedero ? "selected" : ""}>No</option></select></div>
    <div class="campo"><label>Descripción</label><textarea name="descripcion" maxlength="300">${esc(p ? p.descripcion : "")}</textarea></div>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Guardar", accion: async (w) => {
        const d = leerFormulario(w);
        d.perecedero = d.perecedero === "true";
        await api(p ? `/api/productos/${p.id}` : "/api/productos", { metodo: p ? "PUT" : "POST", cuerpo: d });
        aviso("Producto guardado", "exito");
        await cargar();
      }
    }
  ]);
  $("#nuevo").addEventListener("click", () => formulario());
  $("#f-q").addEventListener("input", () => cargar().catch(error));
  $("#filas").addEventListener("click", async (e) => {
    const editar = e.target.closest("[data-editar]");
    const activar = e.target.closest("[data-activar]");
    try {
      if (editar) formulario(lista.find((p) => p.id === editar.dataset.editar));
      if (activar) {
        await api(`/api/productos/${activar.dataset.activar}/estado`, { metodo: "PATCH", cuerpo: { activo: activar.dataset.valor === "true" } });
        await cargar();
      }
    } catch (err) {
      error(err);
    }
  });
  await cargar();
}
