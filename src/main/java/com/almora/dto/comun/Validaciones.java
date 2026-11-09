package com.almora.dto.comun;

/** Reglas de validacion compartidas por los DTO. */
public final class Validaciones {

    private Validaciones() {
    }

    /** Minimo 8 caracteres (maximo 72 por limite de BCrypt), con mayuscula, minuscula y numero. */
    public static final String CLAVE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String CLAVE_MENSAJE =
            "La contrasena debe tener entre 8 y 72 caracteres e incluir mayuscula, minuscula y numero";

    public static final String TELEFONO = "^[0-9+()\\s-]{7,20}$";
    public static final String TELEFONO_MENSAJE = "Telefono invalido (solo numeros, espacios, +, - y parentesis; 7 a 20 caracteres)";

    /** Cedula o NIT: numeros, guion y letras (sin espacios raros). */
    public static final String IDENTIFICACION = "^[0-9A-Za-z.-]{4,20}$";
    public static final String IDENTIFICACION_MENSAJE = "Identificacion invalida (4 a 20 caracteres: numeros, letras, punto o guion)";
}
