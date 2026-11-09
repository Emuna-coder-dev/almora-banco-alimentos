// =====================================================================
//  Cliente de la API de ALMORA.
//  Seguridad: el token de acceso (JWT) vive SOLO en memoria (nunca en localStorage).
//  Cuando vence, se renueva con la cookie HttpOnly de refresco (el navegador la envia sola).
// =====================================================================

let token = null;
let usuario = null;
let renovando = null; // evita dos renovaciones al mismo tiempo (el token de refresco es de un solo uso)

export class ErrorApi extends Error {
  constructor(status, mensaje, detalles) {
    super(mensaje);
    this.status = status;
    this.detalles = detalles;
  }
}

export function usuarioActual() {
  return usuario;
}

export function esAdministrativo() {
  return usuario && (usuario.rol === "ADMIN" || usuario.rol === "SUPERADMIN");
}

export function esSuperAdmin() {
  return usuario && usuario.rol === "SUPERADMIN";
}

function guardarSesion(respuesta) {
  token = respuesta.accessToken;
  usuario = respuesta.usuario;
}

export async function login(correo, password) {
  const r = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ correo, password })
  });
  const datos = await leer(r);
  guardarSesion(datos);
  return datos;
}

export async function registrar(datos) {
  const r = await fetch("/api/auth/registro", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos)
  });
  return leer(r);
}

/** Renueva el token con la cookie de refresco. Devuelve true si hay sesion. */
export async function renovar() {
  if (!renovando) {
    renovando = (async () => {
      try {
        const r = await fetch("/api/auth/refresh", { method: "POST", credentials: "same-origin" });
        if (!r.ok) {
          token = null;
          usuario = null;
          return false;
        }
        guardarSesion(await r.json());
        return true;
      } catch {
        return false;
      } finally {
        setTimeout(() => { renovando = null; }, 0);
      }
    })();
  }
  return renovando;
}

export async function logout() {
  try {
    await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" });
  } finally {
    token = null;
    usuario = null;
  }
}

/** Llamada autenticada. Si el token vencio, se renueva una vez y se reintenta. */
export async function api(ruta, { metodo = "GET", cuerpo, parametros, binario = false } = {}) {
  let url = ruta;
  if (parametros) {
    const q = new URLSearchParams();
    Object.entries(parametros).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") q.append(k, v);
    });
    const texto = q.toString();
    if (texto) url += (url.includes("?") ? "&" : "?") + texto;
  }
  const hacer = () => fetch(url, {
    method: metodo,
    credentials: "same-origin",
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(cuerpo !== undefined ? { "Content-Type": "application/json" } : {})
    },
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined
  });

  let r = await hacer();
  if (r.status === 401 && await renovar()) {
    r = await hacer();
  }
  if (r.status === 401) {
    window.location.href = "/index.html?sesion=expirada";
    throw new ErrorApi(401, "Tu sesion expiro");
  }
  if (binario) {
    if (!r.ok) await leer(r);
    return r;
  }
  return leer(r);
}

/** Descarga un archivo protegido (PDF / Excel) con el token. */
export async function descargar(ruta, nombre) {
  const r = await api(ruta, { binario: true });
  const blob = await r.blob();
  const disp = r.headers.get("Content-Disposition") || "";
  const m = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disp);
  const archivo = m ? decodeURIComponent(m[1] || m[2]) : nombre;
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(blob);
  enlace.download = archivo;
  document.body.appendChild(enlace);
  enlace.click();
  setTimeout(() => {
    URL.revokeObjectURL(enlace.href);
    enlace.remove();
  }, 1000);
}

async function leer(r) {
  const tipo = r.headers.get("Content-Type") || "";
  const datos = tipo.includes("application/json") ? await r.json().catch(() => null) : null;
  if (!r.ok) {
    let mensaje = (datos && datos.mensaje) || "Ocurrio un error (" + r.status + ")";
    if (datos && datos.detalles) {
      mensaje += ": " + Object.values(datos.detalles).join(" | ");
    }
    throw new ErrorApi(r.status, mensaje, datos && datos.detalles);
  }
  return datos;
}
