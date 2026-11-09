// =====================================================================
//  Utilidades de interfaz: escapar texto, iconos, ventanas, avisos y formatos.
//  Todo el texto que viene del servidor pasa por esc() antes de ir al HTML (evita XSS).
// =====================================================================

export function esc(valor) {
  if (valor === null || valor === undefined) return "";
  return String(valor)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

export const $ = (sel, raiz = document) => raiz.querySelector(sel);
export const $$ = (sel, raiz = document) => Array.from(raiz.querySelectorAll(sel));

// ------------------------------------------------------------------ formatos
const fmtNumero = new Intl.NumberFormat("es-CO");
export const num = (n) => fmtNumero.format(n || 0);

export function fecha(valor) {
  if (!valor) return "-";
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    const [a, m, d] = valor.split("-");
    return `${d}/${m}/${a}`;
  }
  return new Date(valor).toLocaleDateString("es-CO", { day: "2-digit", month: "2-digit", year: "numeric" });
}

export function fechaHora(valor) {
  if (!valor) return "-";
  return new Date(valor).toLocaleString("es-CO", {
    day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit"
  });
}

/** Fecha local AAAA-MM-DD (para los filtros de la API). */
export function hoyIso(desplazarDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + desplazarDias);
  return d.toLocaleDateString("en-CA");
}

export function primerDiaMes() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toLocaleDateString("en-CA");
}

const UNIDADES = { UNIDAD: "und", KILOGRAMO: "kg", LIBRA: "lb", LITRO: "L", PAQUETE: "paq", CAJA: "cajas" };
export const unidad = (u) => UNIDADES[u] || "";

export const CATEGORIAS = {
  GRANOS_Y_CEREALES: { nombre: "Granos y cereales", icono: "🍚" },
  LACTEOS: { nombre: "Lácteos", icono: "🥛" },
  CARNES_Y_PROTEINAS: { nombre: "Carnes y proteínas", icono: "🥚" },
  FRUTAS_Y_VERDURAS: { nombre: "Frutas y verduras", icono: "🍅" },
  PANADERIA: { nombre: "Panadería", icono: "🍞" },
  ENLATADOS_Y_CONSERVAS: { nombre: "Enlatados", icono: "🥫" },
  BEBIDAS: { nombre: "Bebidas", icono: "🧃" },
  ACEITES_Y_CONDIMENTOS: { nombre: "Aceites y condimentos", icono: "🫒" },
  ASEO_PERSONAL: { nombre: "Aseo personal", icono: "🧼" },
  OTRO: { nombre: "Otros", icono: "📦" }
};
export const categoria = (c) => (CATEGORIAS[c] || CATEGORIAS.OTRO).nombre;
export const iconoCategoria = (c) => (CATEGORIAS[c] || CATEGORIAS.OTRO).icono;

export const TIPOS_DONANTE = {
  PERSONA_NATURAL: "Persona natural", EMPRESA: "Empresa", SUPERMERCADO: "Supermercado",
  RESTAURANTE: "Restaurante", ORGANIZACION: "Organización", OTRO: "Otro"
};

export function opciones(mapa, seleccionado = "", conVacio = null) {
  let html = conVacio !== null ? `<option value="">${esc(conVacio)}</option>` : "";
  for (const [valor, texto] of Object.entries(mapa)) {
    const t = typeof texto === "object" ? texto.nombre : texto;
    html += `<option value="${esc(valor)}" ${valor === seleccionado ? "selected" : ""}>${esc(t)}</option>`;
  }
  return html;
}

// ------------------------------------------------------------------ insignias
const ESTADO_DONACION = {
  PENDIENTE: ["Pendiente", "ins-gris"], RECIBIDA: ["Recibida", "ins-azul"], ALMACENADA: ["Almacenada", "ins-azul"],
  ASIGNADA: ["Asignada", "ins-morado"], EN_RUTA: ["En ruta", "ins-amarillo"],
  PARCIALMENTE_ENTREGADA: ["Parcialmente entregada", "ins-verde"], ENTREGADA: ["Entregada", "ins-verde"],
  VENCIDA: ["Vencida", "ins-rojo"], ANULADA: ["Anulada", "ins-rojo"]
};
const ESTADO_ENTREGA = {
  PROGRAMADA: ["Programada", "ins-azul"], EN_RUTA: ["En proceso", "ins-amarillo"],
  ENTREGADA: ["Completada", "ins-verde"], CANCELADA: ["Cancelada", "ins-rojo"]
};
const SEMAFORO = {
  VERDE: ["Bueno", "ins-verde"], AMARILLO: ["Bueno", "ins-verde"], ROJO: ["Próximo a vencer", "ins-amarillo"],
  VENCIDO: ["Vencido", "ins-rojo"], SIN_VENCIMIENTO: ["No vence", "ins-gris"]
};
const SEMAFORO_DETALLE = {
  VERDE: ["Verde", "ins-verde"], AMARILLO: ["Amarillo", "ins-amarillo"], ROJO: ["Rojo", "ins-rojo"],
  VENCIDO: ["Vencido", "ins-rojo"], SIN_VENCIMIENTO: ["Sin fecha", "ins-gris"]
};

function insignia(mapa, clave) {
  const [texto, clase] = mapa[clave] || [clave || "-", "ins-gris"];
  return `<span class="insignia ${clase}">${esc(texto)}</span>`;
}
export const insigniaDonacion = (e) => insignia(ESTADO_DONACION, e);
export const insigniaEntrega = (e) => insignia(ESTADO_ENTREGA, e);
export const insigniaStock = (s) => insignia(SEMAFORO, s);
export const insigniaSemaforo = (s) => insignia(SEMAFORO_DETALLE, s);
export const ESTADOS_DONACION = Object.fromEntries(Object.entries(ESTADO_DONACION).map(([k, v]) => [k, v[0]]));
export const ESTADOS_ENTREGA = Object.fromEntries(Object.entries(ESTADO_ENTREGA).map(([k, v]) => [k, v[0]]));

// ------------------------------------------------------------------ avisos
export function aviso(texto, tipo = "") {
  let caja = $(".avisos");
  if (!caja) {
    caja = document.createElement("div");
    caja.className = "avisos";
    document.body.appendChild(caja);
  }
  const a = document.createElement("div");
  a.className = "aviso " + tipo;
  a.textContent = texto;
  caja.appendChild(a);
  setTimeout(() => a.remove(), tipo === "error" ? 7000 : 4000);
}

export function error(e) {
  aviso(e && e.message ? e.message : "Ocurrio un error", "error");
}

// ------------------------------------------------------------------ ventana modal
/**
 * Abre una ventana. contenido = HTML (ya escapado). botones = [{texto, clase, accion}].
 * accion(ventana) puede devolver false para no cerrar. Devuelve el elemento de la ventana.
 */
export function modal(titulo, contenido, botones = [], { ancho = false } = {}) {
  const fondo = document.createElement("div");
  fondo.className = "modal-fondo";
  fondo.innerHTML = `
    <div class="modal ${ancho ? "ancho" : ""}" role="dialog" aria-modal="true" aria-label="${esc(titulo)}">
      <div class="modal-cabecera"><h2>${esc(titulo)}</h2><button class="modal-cerrar" aria-label="Cerrar">&times;</button></div>
      <div class="modal-cuerpo">${contenido}</div>
      ${botones.length ? `<div class="modal-pie"></div>` : ""}
    </div>`;
  const cerrar = () => fondo.remove();
  $(".modal-cerrar", fondo).addEventListener("click", cerrar);
  fondo.addEventListener("click", (ev) => { if (ev.target === fondo) cerrar(); });
  const pie = $(".modal-pie", fondo);
  botones.forEach((b) => {
    const boton = document.createElement("button");
    boton.className = "boton " + (b.clase || "");
    boton.textContent = b.texto;
    boton.addEventListener("click", async () => {
      if (!b.accion) return cerrar();
      boton.disabled = true;
      try {
        const r = await b.accion(fondo);
        if (r !== false) cerrar();
      } catch (e) {
        error(e);
      } finally {
        boton.disabled = false;
      }
    });
    pie.appendChild(boton);
  });
  document.body.appendChild(fondo);
  const primero = $("input, select, textarea", fondo);
  if (primero) primero.focus();
  fondo.cerrar = cerrar;
  return fondo;
}

export function confirmar(titulo, mensaje, textoBoton = "Confirmar", clase = "") {
  return new Promise((resolver) => {
    const v = modal(titulo, `<p>${esc(mensaje)}</p>`, [
      { texto: "Cancelar", clase: "secundario", accion: () => resolver(false) },
      { texto: textoBoton, clase, accion: () => resolver(true) }
    ]);
    $(".modal-cerrar", v).addEventListener("click", () => resolver(false));
  });
}

export function pedirTexto(titulo, etiqueta, { minimo = 0, valor = "" } = {}) {
  return new Promise((resolver) => {
    const v = modal(titulo, `<div class="campo"><label>${esc(etiqueta)}</label><textarea id="texto-pedido">${esc(valor)}</textarea></div>`, [
      { texto: "Cancelar", clase: "secundario", accion: () => resolver(null) },
      {
        texto: "Aceptar", accion: (w) => {
          const t = $("#texto-pedido", w).value.trim();
          if (t.length < minimo) {
            aviso(`Escribe al menos ${minimo} caracteres`, "error");
            return false;
          }
          resolver(t);
        }
      }
    ]);
    $(".modal-cerrar", v).addEventListener("click", () => resolver(null));
  });
}

/** Lee los campos con atributo name de un contenedor y devuelve un objeto (vacios = null). */
export function leerFormulario(raiz) {
  const datos = {};
  $$("[name]", raiz).forEach((c) => {
    const v = c.type === "checkbox" ? c.checked : c.value.trim();
    datos[c.name] = v === "" ? null : v;
  });
  return datos;
}

export function paginacion(p, alCambiar) {
  const div = document.createElement("div");
  div.className = "paginacion";
  if (!p || p.totalPaginas <= 1) {
    div.textContent = p ? `${num(p.totalElementos)} registro(s)` : "";
    return div;
  }
  div.innerHTML = `<span>Página ${p.pagina + 1} de ${p.totalPaginas} · ${num(p.totalElementos)} registros</span>
    <button class="boton secundario pequeno" data-p="${p.pagina - 1}" ${p.pagina === 0 ? "disabled" : ""}>Anterior</button>
    <button class="boton secundario pequeno" data-p="${p.pagina + 1}" ${p.pagina + 1 >= p.totalPaginas ? "disabled" : ""}>Siguiente</button>`;
  $$("button", div).forEach((b) => b.addEventListener("click", () => alCambiar(Number(b.dataset.p))));
  return div;
}

// ------------------------------------------------------------------ iconos (SVG)
export const ICONOS = {
  logo: `<svg viewBox="0 0 64 64" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path d="M32 54C18 44 8 35 8 23.5 8 16 13.8 10 21 10c4.6 0 8.6 2.4 11 6 2.4-3.6 6.4-6 11-6 7.2 0 13 6 13 13.5C56 35 46 44 32 54z"/>
    <path d="M32 46V30" /><path d="M32 34c-6 0-10-4-10-9 6 0 10 4 10 9z" fill="#2fb36b" stroke="#fff"/>
    <path d="M32 31c0-6 4-10 10-10 0 6-4 10-10 10z" fill="#7fd6a0" stroke="#fff"/>
    <path d="M6 44c4 6 10 10 18 12M58 44c-4 6-10 10-18 12" /></svg>`,
  manoCorazon: `<svg viewBox="0 0 80 80" fill="none" stroke="#8fe0b0" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path d="M40 40c-8-6-14-11-14-18 0-4.5 3.4-8 7.6-8 2.7 0 5 1.4 6.4 3.5 1.4-2.1 3.7-3.5 6.4-3.5 4.2 0 7.6 3.5 7.6 8 0 7-6 12-14 18z"/>
    <path d="M40 34v-9M40 28c-3 0-5-2-5-5 3 0 5 2 5 5zM40 26c0-3 2-5 5-5 0 3-2 5-5 5z"/>
    <path d="M10 52h10l12 4h12c2 0 3 1.5 3 3s-1 3-3 3H32M47 59l14-6c2-.8 4 0 4.6 1.8.5 1.5-.2 3-1.6 3.8L44 68H24l-14-4"/></svg>`,
  inicio: `<svg viewBox="0 0 24 24"><path d="M12 3 2 11.5h3V21h5.5v-6h3v6H19v-9.5h3z"/></svg>`,
  donaciones: `<svg viewBox="0 0 24 24"><path d="M20 7h-2.2A3 3 0 0 0 12 4.2 3 3 0 0 0 6.2 7H4a1 1 0 0 0-1 1v3a1 1 0 0 0 1 1h7V7.2h2V12h7a1 1 0 0 0 1-1V8a1 1 0 0 0-1-1zM9 7a1 1 0 1 1 1-1v1H9zm6 0h-1V6a1 1 0 1 1 1 1zM4 13v7a1 1 0 0 0 1 1h6v-8H4zm9 8h6a1 1 0 0 0 1-1v-7h-7v8z"/></svg>`,
  inventario: `<svg viewBox="0 0 24 24"><path d="M12 2 3 6.5v11L12 22l9-4.5v-11L12 2zm0 2.2 6.6 3.3L12 10.8 5.4 7.5 12 4.2zM5 9.2l6 3v7.3l-6-3V9.2zm8 10.3v-7.3l6-3v7.3l-6 3z"/></svg>`,
  distribucion: `<svg viewBox="0 0 24 24"><path d="M1 5h13v11H1zM15 9h4l3 4v3h-7zM5.5 20a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm12 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"/></svg>`,
  organizaciones: `<svg viewBox="0 0 24 24"><path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm7.5-1a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM9 13c-4 0-8 2-8 5v2h16v-2c0-3-4-5-8-5zm7.5-1c-.6 0-1.3.1-2 .2 1.6 1.1 2.5 2.5 2.5 4.3V20h6v-2.5c0-3-3.3-5.5-6.5-5.5z"/></svg>`,
  reportes: `<svg viewBox="0 0 24 24"><path d="M3 21V11h4v10H3zm7 0V4h4v17h-4zm7 0v-7h4v7h-4z"/></svg>`,
  configuracion: `<svg viewBox="0 0 24 24"><path d="M19.4 13a7.5 7.5 0 0 0 0-2l2.1-1.6-2-3.5-2.5 1a7.4 7.4 0 0 0-1.7-1L15 3.3h-4l-.4 2.6a7.4 7.4 0 0 0-1.7 1l-2.5-1-2 3.5L6.5 11a7.5 7.5 0 0 0 0 2l-2.1 1.6 2 3.5 2.5-1c.5.4 1.1.7 1.7 1l.4 2.6h4l.4-2.6c.6-.3 1.2-.6 1.7-1l2.5 1 2-3.5L19.4 13zM12 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z"/></svg>`,
  seguimiento: `<svg viewBox="0 0 24 24"><path d="M12 2a7 7 0 0 0-7 7c0 5.2 7 13 7 13s7-7.8 7-13a7 7 0 0 0-7-7zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z"/></svg>`,
  persona: `<svg viewBox="0 0 24 24"><path d="M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zm0 2c-4.5 0-8 2.3-8 5v2h16v-2c0-2.7-3.5-5-8-5z"/></svg>`,
  flechaAbajo: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6"/></svg>`,
  caja: (c) => `<svg viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.8" stroke-linejoin="round"><path d="M12 3 4 7v10l8 4 8-4V7l-8-4z"/><path d="M4 7l8 4 8-4M12 11v10"/></svg>`,
  cubo: (c) => `<svg viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.8" stroke-linejoin="round"><path d="M12 3 4 7.5v9L12 21l8-4.5v-9L12 3z"/><path d="M4 7.5 12 12l8-4.5M12 12v9"/></svg>`,
  calendario: (c) => `<svg viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.8" stroke-linecap="round"><rect x="3.5" y="5" width="15" height="14" rx="2"/><path d="M3.5 9.5h15M7.5 3v4M14.5 3v4"/><circle cx="18" cy="17" r="4" fill="#f8d462"/><path d="M18 15.2V17l1.2.8"/></svg>`,
  camion: (c) => `<svg viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.8" stroke-linejoin="round"><path d="M2 6h11v10H2zM13 9.5h4.5L21 13v3h-8z"/><circle cx="6" cy="18" r="2" fill="#c3b2f3"/><circle cx="17" cy="18" r="2" fill="#c3b2f3"/></svg>`,
  flecha: (c) => `<svg viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 18 18 6M9 6h9v9"/></svg>`,
  familias: `<svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.2"/><circle cx="16.5" cy="9" r="2.5"/><path d="M3 19c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M14.5 14.2c3.2-.6 6.5 1.2 6.5 4.8"/></svg>`,
  hoja: `<svg viewBox="0 0 24 24"><path d="M5 19c0-8 5-14 15-14 0 10-6 15-14 15"/><path d="M5 19 14 10"/></svg>`,
  mundo: `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.6 2.6 3.8 5.6 3.8 9S14.6 18.4 12 21c-2.6-2.6-3.8-5.6-3.8-9S9.4 5.6 12 3z"/></svg>`
};
