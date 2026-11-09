import { login, registrar, renovar } from "./api.js";
import { $, aviso, error, esc, ICONOS, leerFormulario, opciones, TIPOS_DONANTE } from "./ui.js";

$("#logo-acceso").innerHTML = ICONOS.logo;
$("#tipo-donante").innerHTML = opciones(TIPOS_DONANTE, "PERSONA_NATURAL");

const parametros = new URLSearchParams(location.search);
if (parametros.get("sesion") === "expirada") {
  $("#mensaje-acceso").innerHTML = `<div class="alerta info">Tu sesión terminó. Ingresa de nuevo.</div>`;
} else {
  // Si ya hay una sesion abierta (cookie de refresco valida), entra directo
  renovar().then((ok) => { if (ok) location.href = "/app.html"; });
}

function mostrar(registro) {
  $("#form-login").classList.toggle("oculto", registro);
  $("#form-registro").classList.toggle("oculto", !registro);
  $("#mensaje-acceso").innerHTML = "";
}
$("#ir-registro").addEventListener("click", (e) => { e.preventDefault(); mostrar(true); });
$("#ir-login").addEventListener("click", (e) => { e.preventDefault(); mostrar(false); });

$("#form-login").addEventListener("submit", async (e) => {
  e.preventDefault();
  const boton = e.submitter;
  boton.disabled = true;
  try {
    await login($("#correo").value.trim(), $("#password").value);
    location.href = "/app.html";
  } catch (err) {
    $("#mensaje-acceso").innerHTML = `<div class="alerta">${esc(err.message)}</div>`;
  } finally {
    boton.disabled = false;
  }
});

$("#form-registro").addEventListener("submit", async (e) => {
  e.preventDefault();
  const boton = e.submitter;
  boton.disabled = true;
  try {
    const datos = leerFormulario(e.target);
    await registrar(datos);
    mostrar(false);
    $("#correo").value = datos.correo;
    $("#mensaje-acceso").innerHTML = `<div class="alerta info">¡Cuenta creada! Ya puedes iniciar sesión.</div>`;
    aviso("Registro exitoso", "exito");
  } catch (err) {
    error(err);
  } finally {
    boton.disabled = false;
  }
});
