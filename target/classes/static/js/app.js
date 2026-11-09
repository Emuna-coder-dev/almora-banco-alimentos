// =====================================================================
//  Arranque de la aplicacion: recupera la sesion, arma el menu segun el rol
//  y muestra la vista que indica la direccion (#/inicio, #/donaciones, ...).
// =====================================================================
import { esAdministrativo, logout, renovar, usuarioActual } from "./api.js";
import { $, $$, esc, ICONOS } from "./ui.js";
import * as inicio from "./vistas/inicio.js";
import * as donaciones from "./vistas/donaciones.js";
import * as seguimiento from "./vistas/seguimiento.js";
import * as inventario from "./vistas/inventario.js";
import * as distribucion from "./vistas/distribucion.js";
import * as organizaciones from "./vistas/organizaciones.js";
import * as reportes from "./vistas/reportes.js";
import * as configuracion from "./vistas/configuracion.js";

const RUTAS_ADMIN = [
  ["inicio", "Inicio", inicio],
  ["donaciones", "Donaciones", donaciones],
  ["inventario", "Inventario", inventario],
  ["distribucion", "Distribución", distribucion],
  ["organizaciones", "Organizaciones", organizaciones],
  ["reportes", "Reportes", reportes],
  ["configuracion", "Configuración", configuracion]
];
const RUTAS_DONANTE = [
  ["inicio", "Inicio", inicio],
  ["donaciones", "Mis donaciones", donaciones],
  ["seguimiento", "Seguimiento", seguimiento],
  ["configuracion", "Configuración", configuracion]
];
// El seguimiento tambien existe para administradores (se abre desde una donacion)
const OCULTAS = { seguimiento };

let rutas = [];

async function arrancar() {
  if (!(await renovar())) {
    location.href = "/index.html";
    return;
  }
  const u = usuarioActual();
  rutas = esAdministrativo() ? RUTAS_ADMIN : RUTAS_DONANTE;

  $("#logo").innerHTML = ICONOS.logo;
  $("#logo-pie").innerHTML = ICONOS.manoCorazon;
  $("#avatar").innerHTML = ICONOS.persona;
  $("#flecha-usuario").innerHTML = ICONOS.flechaAbajo;
  $("#usuario-rol").textContent = { SUPERADMIN: "Superadministrador", ADMIN: "Administrador", DONANTE: "Donante" }[u.rol];
  $("#usuario-nombre").textContent = u.nombre;
  $("#usuario-correo").textContent = u.correo;

  $("#menu").innerHTML = rutas.map(([id, nombre]) =>
    `<a href="#/${id}" data-ruta="${id}">${ICONOS[id] || ""}<span>${esc(nombre)}</span></a>`).join("");

  $("#usuario-boton").addEventListener("click", () => $("#usuario-desplegable").classList.toggle("oculto"));
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".usuario-menu")) $("#usuario-desplegable").classList.add("oculto");
  });
  $$("[data-ir]").forEach((b) => b.addEventListener("click", () => {
    location.hash = b.dataset.ir;
    $("#usuario-desplegable").classList.add("oculto");
  }));
  $("#cerrar-sesion").addEventListener("click", async () => {
    await logout();
    location.href = "/index.html";
  });

  $("#app").classList.remove("oculto");
  window.addEventListener("hashchange", mostrarVista);
  mostrarVista();
}

async function mostrarVista() {
  const [ruta, consulta] = (location.hash.replace(/^#\/?/, "") || "inicio").split("?");
  const encontrada = rutas.find(([id]) => id === ruta);
  const vista = encontrada ? encontrada[2] : OCULTAS[ruta];
  if (!vista) {
    location.hash = "#/inicio";
    return;
  }
  $$("#menu a").forEach((a) => a.classList.toggle("activo", a.dataset.ruta === ruta));
  const contenido = $("#contenido");
  contenido.innerHTML = `<p style="color:var(--texto-3)">Cargando…</p>`;
  try {
    await vista.render(contenido, new URLSearchParams(consulta || ""));
  } catch (e) {
    contenido.innerHTML = `<div class="alerta">${esc(e.message || "No se pudo cargar esta sección")}</div>`;
  }
  contenido.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

arrancar();
