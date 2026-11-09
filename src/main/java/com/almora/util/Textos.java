package com.almora.util;

import java.util.regex.Pattern;

public final class Textos {

    private Textos() {
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }

    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** Escapa un texto para usarlo dentro de una expresion regular de MongoDB (evita inyeccion de regex). */
    public static String escaparRegex(String texto) {
        return Pattern.quote(texto == null ? "" : texto.trim());
    }

    public static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
