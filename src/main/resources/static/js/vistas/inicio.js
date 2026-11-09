// Inicio: tablero igual al diseno de referencia (administradores) o resumen personal (donantes).
import { api, esAdministrativo, usuarioActual } from "../api.js";
import {
  $, categoria, esc, fecha, ICONOS, iconoCategoria, insigniaDonacion, insigniaEntrega, insigniaStock, num,
  primerDiaMes, unidad
} from "../ui.js";

const COLORES = ["#1f9d55", "#f2861d", "#f5c02e", "#4a90e2", "#8b5cf6"];

export async function render(c) {
  if (esAdministrativo()) {
    await tableroAdministrador(c);
  } else {
    await tableroDonante(c);
  }
}

// ------------------------------------------------------------------ piezas comunes
function bienvenida(titulo, texto) {
  return `<section class="bienvenida">
    <div class="bienvenida-imagen" role="img" aria-label="Caja de madera con verduras y frutas frescas"></div>
    <h1>${esc(titulo)}</h1>
    <p>${texto}</p>
  </section>`;
}

function indicador(clase, icono, color, titulo, valor, nota, enlace) {
  return `<div class="indicador ${clase}">
    <div class="icono">${icono}</div>
    <div>
      <div class="titulo">${esc(titulo)}</div>
      <div class="valor">${esc(valor)}</div>
      <div class="nota">${esc(nota)}</div>
    </div>
    <a class="flecha" href="${enlace}" aria-label="Ver ${esc(titulo)}">${ICONOS.flecha(color)}</a>
  </div>`;
}

function tarjetaImpacto(texto) {
  return `<section class="impacto">
    <div class="impacto-imagen" aria-hidden="true"></div>
    <h2>Nuestro impacto</h2>
    <p>${texto}</p>
    <div class="impacto-items">
      <div>${ICONOS.familias}Más familias<br>beneficiadas</div>
      <div>${ICONOS.hoja}Menos<br>desperdicio</div>
      <div>${ICONOS.mundo}Un mundo<br>más solidario</div>
    </div>
  </section>`;
}

/** Dona en SVG: cada segmento es un circulo con stroke-dasharray. */
function dona(datos) {
  const total = datos.reduce((s, d) => s + d.valor, 0);
  const r = 70, circ = 2 * Math.PI * r;
  let acumulado = 0;
  const segmentos = total === 0
    ? `<circle cx="100" cy="100" r="${r}" fill="none" stroke="#e8eeeb" stroke-width="40"/>`
    : datos.map((d, i) => {
        const largo = (d.valor / total) * circ;
        const s = `<circle cx="100" cy="100" r="${r}" fill="none" stroke="${COLORES[i % COLORES.length]}" stroke-width="40"
          stroke-dasharray="${largo} ${circ - largo}" stroke-dashoffset="${-acumulado}"><title>${esc(d.nombre)}: ${num(d.valor)}</title></circle>`;
        acumulado += largo;
        return s;
      }).join("");
  const leyenda = total === 0
    ? `<div style="color:var(--texto-3)">Aún no hay donaciones registradas.</div>`
    : datos.map((d, i) => `<div><i style="background:${COLORES[i % COLORES.length]}"></i><span>${esc(d.nombre)}</span>
        <span>${Math.round((d.valor / total) * 100)}%</span></div>`).join("");
  return `<div class="dona-contenedor">
    <div class="dona"><svg viewBox="0 0 200 200" role="img" aria-label="Distribución por tipo de producto">${segmentos}</svg>
      <div class="dona-centro"><div><b>${num(total)}</b><span>productos</span></div></div></div>
    <div class="leyenda">${leyenda}</div>
  </div>`;
}

/** Las 4 categorias con mas unidades y el resto agrupado en "Otros". */
function agrupar(mapa) {
  const lista = Object.entries(mapa || {}).map(([k, v]) => ({ clave: k, nombre: categoria(k), valor: v }))
    .filter((d) => d.valor > 0).sort((a, b) => b.valor - a.valor);
  if (lista.length <= 5) return lista;
  const otros = lista.slice(4).reduce((s, d) => s + d.valor, 0);
  return [...lista.slice(0, 4), { clave: "OTROS", nombre: "Otros", valor: otros }];
}

function unidadesEntrega(e) {
  const unidades = new Set(e.items.map((i) => i.unidadMedida));
  const u = unidades.size === 1 ? unidad([...unidades][0]) : "und";
  return `${num(e.totalUnidades)} ${u}`;
}

// ------------------------------------------------------------------ administradores
async function tableroAdministrador(c) {
  const mes = primerDiaMes();
  const [ind, donMes, inv, alertas, entMes, recientes, lotes] = await Promise.all([
    api("/api/reportes/indicadores"),
    api("/api/donaciones", { parametros: { desde: mes, tamano: 1 } }),
    api("/api/inventario"),
    api("/api/inventario/alertas", { parametros: { dias: 7 } }),
    api("/api/entregas", { parametros: { estado: "ENTREGADA", desde: mes, tamano: 1 } }),
    api("/api/entregas", { parametros: { tamano: 4 } }),
    api("/api/inventario/lotes", { parametros: { estado: "DISPONIBLE", tamano: 5 } })
  ]);
  const stock = inv.reduce((s, p) => s + p.stockDisponible, 0);

  c.innerHTML = `
    ${bienvenida("¡Bienvenido al Banco de Alimentos!",
      "Aquí puedes gestionar las donaciones, controlar el inventario<br>y realizar la distribución de alimentos a las comunidades<br>que más lo necesitan.")}

    <section class="indicadores">
      ${indicador("ind-verde", ICONOS.caja("#1d6b44"), "#1e9e5a", "Donaciones recibidas", num(ind.totalDonaciones), `+${num(donMes.totalElementos)} este mes`, "#/donaciones")}
      ${indicador("ind-azul", ICONOS.cubo("#2f62b8"), "#3b7be0", "Productos en inventario", num(stock), "unidades disponibles", "#/inventario")}
      ${indicador("ind-amarillo", ICONOS.calendario("#6b4d05"), "#f2a21d", "Alimentos próximos a vencer", num(alertas.totalLotes), "en los próximos 7 días", "#/inventario?vista=alertas")}
      ${indicador("ind-morado", ICONOS.camion("#5b3cc4"), "#8b5cf6", "Distribuciones realizadas", num(entMes.totalElementos), "este mes", "#/distribucion")}
    </section>

    <div class="fila-2">
      <section class="tarjeta">
        <div class="tarjeta-titulo">
          <h2>Donaciones por tipo de producto</h2>
          <select id="fuente-dona" aria-label="Datos del gráfico" style="border:0;padding:2px;color:var(--texto-2)">
            <option value="recibidas">Recibidas</option><option value="bodega">En inventario</option>
          </select>
        </div>
        <div id="dona"></div>
      </section>
      <section class="tarjeta">
        <div class="tarjeta-titulo"><h2>Distribuciones recientes</h2><a class="enlace-pequeno" href="#/distribucion">Ver todas</a></div>
        <div class="tabla-envoltura"><table class="tabla">
          <thead><tr><th>Fecha</th><th>Organización</th><th>Productos</th><th>Estado</th></tr></thead>
          <tbody>${recientes.contenido.length ? recientes.contenido.map((e) => `<tr>
            <td>${fecha(e.fechaEntrega || e.fechaProgramada)}</td><td>${esc(e.fundacionNombre)}</td>
            <td>${unidadesEntrega(e)}</td><td>${insigniaEntrega(e.estado)}</td></tr>`).join("")
            : `<tr><td colspan="4" class="vacio">Aún no hay distribuciones.</td></tr>`}</tbody>
        </table></div>
      </section>
    </div>

    <div class="fila-2b">
      <section class="tarjeta">
        <div class="tarjeta-titulo"><h2>Productos en stock <small>(próximos a salir · FEFO)</small></h2><a class="enlace-pequeno" href="#/inventario">Ver inventario</a></div>
        <div class="tabla-envoltura"><table class="tabla">
          <thead><tr><th>Producto</th><th>Cantidad</th><th>Fecha de vencimiento</th><th>Estado</th></tr></thead>
          <tbody>${lotes.contenido.length ? lotes.contenido.map((l) => `<tr>
            <td><div class="producto-celda"><span class="producto-icono">${iconoCategoria(l.categoria)}</span>${esc(l.nombreProducto)}</div></td>
            <td>${num(l.cantidadDisponible)} ${unidad(l.unidadMedida)}</td>
            <td>${l.fechaVencimiento ? fecha(l.fechaVencimiento) : "No vence"}</td>
            <td>${insigniaStock(l.semaforo)}</td></tr>`).join("")
            : `<tr><td colspan="4" class="vacio">El inventario está vacío.</td></tr>`}</tbody>
        </table></div>
      </section>
      ${tarjetaImpacto("Cada donación cuenta. Gracias a la gestión eficiente de los alimentos, ayudamos a construir comunidades más fuertes y saludables.")}
    </div>`;

  const pintarDona = () => {
    const fuente = $("#fuente-dona").value === "bodega" ? ind.unidadesEnBodegaPorCategoria : ind.unidadesRecibidasPorCategoria;
    $("#dona").innerHTML = dona(agrupar(fuente));
  };
  $("#fuente-dona").addEventListener("change", pintarDona);
  pintarDona();
}

// ------------------------------------------------------------------ donantes
async function tableroDonante(c) {
  const u = usuarioActual();
  const [impacto, mias] = await Promise.all([
    api("/api/perfil/impacto"),
    api("/api/donaciones", { parametros: { tamano: 5 } })
  ]);
  const fundaciones = impacto.fundacionesAtendidas || [];

  c.innerHTML = `
    ${bienvenida(`¡Hola, ${u.nombre.split(" ")[0]}!`,
      "Gracias por compartir. Aquí puedes registrar tus donaciones,<br>seguir su ruta hasta la fundación que las recibe<br>y descargar tus certificados.")}
    ${u.debeCambiarPassword ? `<div class="alerta" style="margin-top:16px">Estás usando una contraseña temporal. <a href="#/configuracion">Cámbiala aquí</a>.</div>` : ""}

    <section class="indicadores">
      ${indicador("ind-verde", ICONOS.caja("#1d6b44"), "#1e9e5a", "Mis donaciones", num(impacto.totalDonaciones), "registradas", "#/donaciones")}
      ${indicador("ind-azul", ICONOS.cubo("#2f62b8"), "#3b7be0", "Unidades donadas", num(impacto.unidadesDonadas), `${num(impacto.unidadesEnBodega)} en bodega`, "#/donaciones")}
      ${indicador("ind-amarillo", ICONOS.calendario("#6b4d05"), "#f2a21d", "Certificados", num(impacto.certificadosDisponibles), "disponibles para descargar", "#/donaciones")}
      ${indicador("ind-morado", ICONOS.camion("#5b3cc4"), "#8b5cf6", "Unidades entregadas", num(impacto.unidadesEntregadas), `a ${fundaciones.length} fundación(es)`, "#/seguimiento")}
    </section>

    <div class="fila-2b">
      <section class="tarjeta">
        <div class="tarjeta-titulo"><h2>Mis donaciones recientes</h2>
          <a class="boton pequeno" href="#/donaciones?nueva=1">+ Registrar donación</a></div>
        <div class="tabla-envoltura"><table class="tabla">
          <thead><tr><th>Código</th><th>Fecha</th><th>Unidades</th><th>Estado</th><th></th></tr></thead>
          <tbody>${mias.contenido.length ? mias.contenido.map((d) => `<tr>
            <td>${esc(d.codigo)}</td><td>${fecha(d.fechaRegistro)}</td><td>${num(d.totalUnidades)}</td>
            <td>${insigniaDonacion(d.estado)}</td>
            <td><a class="enlace-pequeno" href="#/seguimiento?codigo=${encodeURIComponent(d.codigo)}">Ver ruta</a></td></tr>`).join("")
            : `<tr><td colspan="5" class="vacio">Aún no has registrado donaciones.</td></tr>`}</tbody>
        </table></div>
      </section>
      ${tarjetaImpacto(fundaciones.length
        ? `Tus donaciones ya llegaron a: <b>${fundaciones.map(esc).join(", ")}</b>. ¡Gracias por construir comunidades más fuertes!`
        : "Cada donación cuenta. Cuando tus productos lleguen a una fundación, la verás aquí.")}
    </div>`;
}
