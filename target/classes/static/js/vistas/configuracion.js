// Configuracion: mi perfil y contrasena (todos); donantes (admins); administradores (superadmin);
// auditoria (admins) y respaldos (superadmin).
import { api, esAdministrativo, esSuperAdmin, logout, usuarioActual } from "../api.js";
import {
  $, $$, aviso, confirmar, error, esc, fecha, fechaHora, leerFormulario, modal, opciones, paginacion, TIPOS_DONANTE
} from "../ui.js";

export async function render(c) {
  const pestanas = [["perfil", "Mi perfil"]];
  if (esAdministrativo()) pestanas.push(["donantes", "Donantes"]);
  if (esSuperAdmin()) pestanas.push(["administradores", "Administradores"]);
  if (esAdministrativo()) pestanas.push(["auditoria", "Auditoría"]);
  if (esSuperAdmin()) pestanas.push(["respaldos", "Respaldos"]);

  c.innerHTML = `
    <div class="encabezado-pagina"><div><h1>Configuración</h1><p>Tu cuenta${esAdministrativo() ? ", usuarios y control del sistema" : ""}.</p></div></div>
    <div class="tarjeta">
      <div class="pestanas">${pestanas.map(([id, t]) => `<button data-pestana="${id}">${t}</button>`).join("")}</div>
      <div id="panel"></div>
    </div>`;
  const vistas = { perfil, donantes, administradores, auditoria, respaldos };
  const abrir = (id) => {
    $$("[data-pestana]", c).forEach((b) => b.classList.toggle("activa", b.dataset.pestana === id));
    vistas[id]($("#panel", c)).catch(error);
  };
  $$("[data-pestana]", c).forEach((b) => b.addEventListener("click", () => abrir(b.dataset.pestana)));
  abrir("perfil");
}

// ------------------------------------------------------------------ mi perfil
async function perfil(panel) {
  const u = await api("/api/perfil");
  const donante = u.rol === "DONANTE";
  panel.innerHTML = `
    ${u.debeCambiarPassword ? `<div class="alerta">Estás usando una contraseña temporal. Cámbiala abajo.</div>` : ""}
    <div class="fila-2" style="margin:0">
      <form id="form-perfil">
        <h3 style="font-size:15px;margin-bottom:12px">Mis datos</h3>
        <div class="campo"><label>Correo</label><input value="${esc(u.correo)}" disabled></div>
        <div class="campo"><label>Nombre</label><input name="nombre" value="${esc(u.nombre)}"></div>
        <div class="campo"><label>Teléfono</label><input name="telefono" value="${esc(u.telefono || "")}"></div>
        ${donante ? `<div class="campo"><label>Dirección</label><input name="direccion" value="${esc(u.direccion || "")}"></div>
          <div class="fila-campos"><div class="campo"><label>Ciudad</label><input name="ciudad" value="${esc(u.ciudad || "")}"></div>
          <div class="campo"><label>Persona de contacto</label><input name="personaContacto" value="${esc(u.personaContacto || "")}"></div></div>` : ""}
        <button class="boton" type="submit">Guardar</button>
      </form>
      <form id="form-clave">
        <h3 style="font-size:15px;margin-bottom:12px">Cambiar contraseña</h3>
        <div class="campo"><label>Contraseña actual</label><input type="password" name="actual" autocomplete="current-password"></div>
        <div class="campo"><label>Nueva contraseña <small>(8+ caracteres, mayúscula, minúscula y número)</small></label><input type="password" name="nueva" autocomplete="new-password"></div>
        <button class="boton" type="submit">Cambiar contraseña</button>
        <p style="color:var(--texto-3);font-size:12.5px">Al cambiarla se cierran tus sesiones y vuelves a ingresar.</p>
      </form>
    </div>`;
  $("#form-perfil").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await api("/api/perfil", { metodo: "PUT", cuerpo: leerFormulario(e.target) });
      aviso("Datos guardados", "exito");
    } catch (err) {
      error(err);
    }
  });
  $("#form-clave").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await api("/api/perfil/password", { metodo: "POST", cuerpo: leerFormulario(e.target) });
      aviso("Contraseña actualizada. Ingresa de nuevo.", "exito");
      await logout();
      setTimeout(() => { location.href = "/index.html"; }, 1200);
    } catch (err) {
      error(err);
    }
  });
}

// ------------------------------------------------------------------ cuentas (donantes y administradores)
function mostrarClaveTemporal(r) {
  if (r.passwordTemporal) {
    modal("Clave temporal", `<p>${esc(r.mensaje)}</p>
      <p>Correo: <b>${esc(r.usuario.correo)}</b><br>Clave temporal: <b style="font-family:monospace;font-size:16px">${esc(r.passwordTemporal)}</b></p>
      <div class="alerta">Cópiala ahora: no se volverá a mostrar. El usuario deberá cambiarla al ingresar.</div>`,
      [{ texto: "Entendido" }]);
  } else {
    aviso(r.mensaje, "exito");
  }
}

function tablaCuentas(lista, esAdmin) {
  const yo = usuarioActual();
  return lista.length ? lista.map((u) => `<tr>
    <td><b>${esc(u.nombre)}</b><br><small style="color:var(--texto-3)">${esc(u.correo)}</small></td>
    <td>${esAdmin ? esc(u.rol === "SUPERADMIN" ? "Superadministrador" : "Administrador") : esc(TIPOS_DONANTE[u.tipoDonante] || "-")}</td>
    <td>${esc(esAdmin ? (u.cargo || "-") : (u.identificacion || "-"))}</td>
    <td>${esc(u.telefono || "-")}</td>
    <td>${u.activo ? '<span class="insignia ins-verde">Activa</span>' : '<span class="insignia ins-gris">Inactiva</span>'}</td>
    <td>${u.ultimoAcceso ? fechaHora(u.ultimoAcceso) : "Nunca"}</td>
    <td><div class="acciones">${u.rol === "SUPERADMIN" || u.id === yo.id ? "" : `
      <button class="boton secundario pequeno" data-accion="editar" data-id="${esc(u.id)}">Editar</button>
      <button class="boton secundario pequeno" data-accion="estado" data-id="${esc(u.id)}">${u.activo ? "Desactivar" : "Activar"}</button>
      <button class="boton secundario pequeno" data-accion="clave" data-id="${esc(u.id)}">Restablecer clave</button>
      ${esSuperAdmin() ? `<button class="boton secundario pequeno" data-accion="rol" data-id="${esc(u.id)}">Hacer ${u.rol === "ADMIN" ? "donante" : "administrador"}</button>` : ""}`}
    </div></td></tr>`).join("") : `<tr><td colspan="7" class="vacio">No hay cuentas.</td></tr>`;
}

async function accionesCuenta(e, lista, recargar) {
  const b = e.target.closest("button[data-accion]");
  if (!b) return;
  const u = lista.find((x) => x.id === b.dataset.id);
  try {
    if (b.dataset.accion === "editar") return editarCuenta(u, recargar);
    if (b.dataset.accion === "estado") {
      await api(`/api/usuarios/${u.id}/estado`, { metodo: "PATCH", cuerpo: { activo: !u.activo } });
    }
    if (b.dataset.accion === "clave") {
      if (!(await confirmar("Restablecer clave", `Se generará una clave temporal para ${u.correo} y se cerrarán sus sesiones.`))) return;
      mostrarClaveTemporal(await api(`/api/usuarios/${u.id}/restablecer-password`, { metodo: "POST" }));
    }
    if (b.dataset.accion === "rol") {
      const nuevo = u.rol === "ADMIN" ? "DONANTE" : "ADMIN";
      if (!(await confirmar("Cambiar rol", `${u.nombre} pasará a ser ${nuevo === "ADMIN" ? "administrador" : "donante"}. Sus sesiones se cerrarán.`))) return;
      await api(`/api/usuarios/${u.id}/rol`, { metodo: "PATCH", cuerpo: { rol: nuevo } });
      aviso("Rol actualizado", "exito");
    }
    await recargar();
  } catch (err) {
    error(err);
  }
}

function editarCuenta(u, recargar) {
  const donante = u.rol === "DONANTE";
  modal(`Editar ${u.nombre}`, `
    <div class="fila-campos"><div class="campo"><label>Nombre</label><input name="nombre" value="${esc(u.nombre)}"></div>
      <div class="campo"><label>Correo</label><input name="correo" type="email" value="${esc(u.correo)}"></div></div>
    <div class="fila-campos"><div class="campo"><label>Teléfono</label><input name="telefono" value="${esc(u.telefono || "")}"></div>
      ${donante ? `<div class="campo"><label>Tipo</label><select name="tipoDonante">${opciones(TIPOS_DONANTE, u.tipoDonante)}</select></div>`
        : `<div class="campo"><label>Cargo</label><input name="cargo" value="${esc(u.cargo || "")}"></div>`}</div>
    ${donante ? `<div class="fila-campos"><div class="campo"><label>Cédula o NIT</label><input name="identificacion" value="${esc(u.identificacion || "")}"></div>
      <div class="campo"><label>Persona de contacto</label><input name="personaContacto" value="${esc(u.personaContacto || "")}"></div></div>
      <div class="fila-campos"><div class="campo"><label>Dirección</label><input name="direccion" value="${esc(u.direccion || "")}"></div>
      <div class="campo"><label>Ciudad</label><input name="ciudad" value="${esc(u.ciudad || "")}"></div></div>` : ""}
    <p style="color:var(--texto-3);font-size:12.5px">Última actualización: ${fechaHora(u.fechaActualizacion)}</p>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Guardar cambios", accion: async (w) => {
        await api(`/api/usuarios/${u.id}`, { metodo: "PUT", cuerpo: leerFormulario(w) });
        aviso("Cuenta actualizada", "exito");
        await recargar();
      }
    }
  ]);
}

async function donantes(panel) {
  let pagina = 0;
  let lista = [];
  panel.innerHTML = `
    <div class="filtros" style="justify-content:space-between">
      <label>Buscar<input id="f-q" placeholder="Nombre, NIT o correo"></label>
      <button class="boton" id="nuevo">+ Registrar donante</button>
    </div>
    <div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Donante</th><th>Tipo</th><th>Identificación</th><th>Teléfono</th><th>Estado</th><th>Último acceso</th><th>Acciones</th></tr></thead>
      <tbody id="filas"></tbody></table></div><div id="paginas"></div>`;
  const cargar = async () => {
    const p = await api("/api/usuarios/donantes", { parametros: { q: $("#f-q").value.trim(), pagina, tamano: 15 } });
    lista = p.contenido;
    $("#filas").innerHTML = tablaCuentas(lista, false);
    $("#paginas").replaceChildren(paginacion(p, (n) => { pagina = n; cargar().catch(error); }));
  };
  $("#f-q").addEventListener("keydown", (e) => { if (e.key === "Enter") { pagina = 0; cargar().catch(error); } });
  $("#filas").addEventListener("click", (e) => accionesCuenta(e, lista, cargar));
  $("#nuevo").addEventListener("click", () => modal("Registrar donante", `
    <div class="campo"><label>Nombre o razón social *</label><input name="nombre"></div>
    <div class="fila-campos"><div class="campo"><label>Tipo *</label><select name="tipoDonante">${opciones(TIPOS_DONANTE, "EMPRESA")}</select></div>
      <div class="campo"><label>Cédula o NIT</label><input name="identificacion"></div></div>
    <div class="fila-campos"><div class="campo"><label>Correo *</label><input name="correo" type="email"></div>
      <div class="campo"><label>Teléfono *</label><input name="telefono"></div></div>
    <div class="fila-campos"><div class="campo"><label>Dirección *</label><input name="direccion"></div>
      <div class="campo"><label>Ciudad</label><input name="ciudad" value="Cartagena"></div></div>
    <div class="campo"><label>Persona de contacto</label><input name="personaContacto"></div>
    <div class="alerta info">El sistema genera una clave temporal para que el donante pueda entrar a ver sus donaciones y certificados.</div>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Registrar", accion: async (w) => {
        mostrarClaveTemporal(await api("/api/usuarios/donantes", { metodo: "POST", cuerpo: leerFormulario(w) }));
        await cargar();
      }
    }
  ]));
  await cargar();
}

async function administradores(panel) {
  let lista = [];
  panel.innerHTML = `
    <div class="filtros" style="justify-content:flex-end"><button class="boton" id="nuevo">+ Crear administrador</button></div>
    <div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Administrador</th><th>Rol</th><th>Cargo</th><th>Teléfono</th><th>Estado</th><th>Último acceso</th><th>Acciones</th></tr></thead>
      <tbody id="filas"></tbody></table></div>`;
  const cargar = async () => {
    lista = (await api("/api/usuarios/administradores", { parametros: { tamano: 100 } })).contenido;
    $("#filas").innerHTML = tablaCuentas(lista, true);
  };
  $("#filas").addEventListener("click", (e) => accionesCuenta(e, lista, cargar));
  $("#nuevo").addEventListener("click", () => modal("Crear administrador", `
    <div class="campo"><label>Nombre completo *</label><input name="nombre"></div>
    <div class="fila-campos"><div class="campo"><label>Correo *</label><input name="correo" type="email"></div>
      <div class="campo"><label>Teléfono</label><input name="telefono"></div></div>
    <div class="campo"><label>Cargo</label><input name="cargo"></div>
    <div class="alerta info">Se generará una clave temporal que el administrador cambiará en su primer ingreso.</div>`, [
    { texto: "Cancelar", clase: "secundario" },
    {
      texto: "Crear", accion: async (w) => {
        mostrarClaveTemporal(await api("/api/usuarios/administradores", { metodo: "POST", cuerpo: leerFormulario(w) }));
        await cargar();
      }
    }
  ]));
  await cargar();
}

// ------------------------------------------------------------------ auditoria y respaldos
async function auditoria(panel) {
  let pagina = 0;
  const ACCIONES = ["CREAR", "ACTUALIZAR", "CAMBIAR_ESTADO", "ANULAR", "ACTIVAR", "DESACTIVAR", "CAMBIAR_ROL", "CAMBIAR_PASSWORD",
    "EXPORTAR", "LOGIN_EXITOSO", "LOGIN_FALLIDO", "CUENTA_BLOQUEADA", "LOGOUT", "REUTILIZACION_TOKEN", "PROCESO_AUTOMATICO"];
  panel.innerHTML = `
    <div class="filtros">
      <label>Acción<select id="f-accion"><option value="">Todas</option>${ACCIONES.map((a) => `<option>${a}</option>`).join("")}</select></label>
      <label>Entidad<select id="f-entidad"><option value="">Todas</option>${["Usuario", "Donacion", "Producto", "Lote", "Entrega", "Fundacion", "Reporte", "Respaldo", "Sesion"]
        .map((x) => `<option>${x}</option>`).join("")}</select></label>
      <label>Usuario<input id="f-usuario" placeholder="correo"></label>
      <label>Desde<input type="date" id="f-desde"></label>
      <button class="boton secundario" id="filtrar">Filtrar</button>
    </div>
    <div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Fecha</th><th>Acción</th><th>Entidad</th><th>Detalle</th><th>Usuario</th><th>IP</th></tr></thead>
      <tbody id="filas"></tbody></table></div><div id="paginas"></div>`;
  const cargar = async () => {
    const p = await api("/api/auditoria", {
      parametros: { accion: $("#f-accion").value, entidad: $("#f-entidad").value, usuario: $("#f-usuario").value.trim(), desde: $("#f-desde").value, pagina, tamano: 30 }
    });
    $("#filas").innerHTML = p.contenido.length ? p.contenido.map((r) => `<tr>
      <td>${fechaHora(r.fecha)}</td><td><span class="insignia ${r.accion === "ANULAR" || r.accion.includes("FALLIDO") ? "ins-rojo" : "ins-gris"}">${esc(r.accion)}</span></td>
      <td>${esc(r.entidad || "")}</td><td style="max-width:320px">${esc(r.detalle || "")}</td>
      <td>${esc(r.usuarioCorreo || r.rol || "")}</td><td>${esc(r.ip || "")}</td></tr>`).join("")
      : `<tr><td colspan="6" class="vacio">Sin registros.</td></tr>`;
    $("#paginas").replaceChildren(paginacion(p, (n) => { pagina = n; cargar().catch(error); }));
  };
  $("#filtrar").addEventListener("click", () => { pagina = 0; cargar().catch(error); });
  await cargar();
}

async function respaldos(panel) {
  panel.innerHTML = `
    <div class="alerta info">Se crea un respaldo automático cada domingo a las 2:00 a. m. en la carpeta <b>respaldos</b> del proyecto (se conservan los 8 más recientes).</div>
    <div class="filtros" style="justify-content:flex-end"><button class="boton" id="crear">Crear respaldo ahora</button></div>
    <div class="tabla-envoltura"><table class="tabla"><thead><tr><th>Archivo</th><th>Tamaño</th><th>Fecha</th></tr></thead><tbody id="filas"></tbody></table></div>`;
  const cargar = async () => {
    const lista = await api("/api/respaldos");
    $("#filas").innerHTML = lista.length ? lista.map((r) => `<tr><td>${esc(r.archivo)}</td>
      <td>${(r.bytes / 1024).toFixed(1)} KB</td><td>${r.fecha ? fechaHora(r.fecha) : "-"}</td></tr>`).join("")
      : `<tr><td colspan="3" class="vacio">Aún no hay respaldos.</td></tr>`;
  };
  $("#crear").addEventListener("click", async () => {
    try {
      const r = await api("/api/respaldos", { metodo: "POST" });
      aviso(`Respaldo creado: ${r.archivo}`, "exito");
      await cargar();
    } catch (err) {
      error(err);
    }
  });
  await cargar();
}
