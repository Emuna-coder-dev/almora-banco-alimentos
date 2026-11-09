// Organizaciones: fundaciones que reciben las donaciones (destino de las entregas).
import { api } from "../api.js";
import { $, aviso, error, esc, fecha, insigniaEntrega, leerFormulario, modal, num } from "../ui.js";

let lista = [];

export async function render(c) {
  c.innerHTML = `
    <div class="encabezado-pagina">
      <div><h1>Organizaciones</h1><p>Fundaciones y comunidades que reciben los alimentos.</p></div>
      <button class="boton" id="nueva">+ Nueva fundación</button>
    </div>
    <div class="tarjeta">
      <div class="filtros"><label>Buscar<input id="f-q" placeholder="Nombre"></label>
        <label>Estado<select id="f-activa"><option value="">Todas</option><option value="true">Activas</option><option value="false">Inactivas</option></select></label></div>
      <div class="tabla-envoltura"><table class="tabla">
        <thead><tr><th>Fundación</th><th>NIT</th><th>Contacto</th><th>Dirección</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody id="filas"></tbody></table></div>
    </div>`;
  $("#nueva").addEventListener("click", () => formulario(c));
  $("#f-q").addEventListener("input", () => cargar(c).catch(error));
  $("#f-activa").addEventListener("change", () => cargar(c).catch(error));
  $("#filas").addEventListener("click", async (e) => {
    const b = e.target.closest("button[data-accion]");
    if (!b) return;
    const f = lista.find((x) => x.id === b.dataset.id);
    try {
      if (b.dataset.accion === "editar") formulario(c, f);
      if (b.dataset.accion === "estado") {
        await api(`/api/fundaciones/${f.id}/estado`, { metodo: "PATCH", cuerpo: { activa: !f.activa } });
        await cargar(c);
      }
      if (b.dataset.accion === "historial") await historial(f);
    } catch (err) {
      error(err);
    }
  });
  await cargar(c);
}

async function cargar(c) {
  lista = await api("/api/fundaciones", { parametros: { q: $("#f-q", c).value.trim(), activa: $("#f-activa", c).value } });
  $("#filas", c).innerHTML = lista.length ? lista.map((f) => `<tr>
    <td><b>${esc(f.nombre)}</b></td><td>${esc(f.nit || "-")}</td>
    <td>${esc(f.personaContacto)}<br><small style="color:var(--texto-3)">${esc(f.telefono)}${f.correo ? " · " + esc(f.correo) : ""}</small></td>
    <td>${esc(f.direccion)}, ${esc(f.ciudad)}</td>
    <td>${f.activa ? '<span class="insignia ins-verde">Activa</span>' : '<span class="insignia ins-gris">Inactiva</span>'}</td>
    <td><div class="acciones">
      <button class="boton secundario pequeno" data-accion="historial" data-id="${esc(f.id)}">Entregas</button>
      <button class="boton secundario pequeno" data-accion="editar" data-id="${esc(f.id)}">Editar</button>
      <button class="boton secundario pequeno" data-accion="estado" data-id="${esc(f.id)}">${f.activa ? "Desactivar" : "Activar"}</button></div></td>
  </tr>`).join("") : `<tr><td colspan="6" class="vacio">No hay fundaciones registradas.</td></tr>`;
}

function formulario(c, f = null) {
  modal(f ? "Editar fundación" : "Nueva fundación", `
    <div class="campo"><label>Nombre *</label><input name="nombre" value="${esc(f ? f.nombre : "")}"></div>
    <div class="fila-campos">
      <div class="campo"><label>NIT</label><input name="nit" value="${esc(f ? f.nit : "")}"></div>
      <div class="campo"><label>Persona de contacto *</label><input name="personaContacto" value="${esc(f ? f.personaContacto : "")}"></div>
    </div>
    <div class="fila-campos">
      <div class="campo"><label>Teléfono *</label><input name="telefono" value="${esc(f ? f.telefono : "")}"></div>
      <div class="campo"><label>Correo</label><input name="correo" type="email" value="${esc(f ? f.correo : "")}"></div>
    </div>
    <div class="fila-campos">
      <div class="campo"><label>Dirección *</label><input name="direccion" value="${esc(f ? f.direccion : "")}"></div>
      <div class="campo"><label>Ciudad *</label><input name="ciudad" value="${esc(f ? f.ciudad : "Cartagena")}"></div>
    </div>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Guardar", accion: async (w) => {
        await api(f ? `/api/fundaciones/${f.id}` : "/api/fundaciones", { metodo: f ? "PUT" : "POST", cuerpo: leerFormulario(w) });
        aviso("Fundación guardada", "exito");
        await cargar(c);
      }
    }
  ]);
}

async function historial(f) {
  const p = await api(`/api/fundaciones/${f.id}/entregas`, { parametros: { tamano: 50 } });
  modal(`Entregas a ${f.nombre}`, p.contenido.length ? `<div class="tabla-envoltura"><table class="tabla">
    <thead><tr><th>Código</th><th>Fecha</th><th>Unidades</th><th>Estado</th></tr></thead>
    <tbody>${p.contenido.map((e) => `<tr><td>${esc(e.codigo)}</td><td>${fecha(e.fechaEntrega || e.fechaProgramada)}</td>
      <td>${num(e.totalUnidades)}</td><td>${insigniaEntrega(e.estado)}</td></tr>`).join("")}</tbody></table></div>`
    : `<p>Esta fundación aún no ha recibido entregas.</p>`, [{ texto: "Cerrar", clase: "secundario" }]);
}
