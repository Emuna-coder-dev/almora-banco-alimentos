package com.almora.util;

import java.security.SecureRandom;

/** Genera claves temporales que cumplen la politica (mayuscula, minuscula y numero, 12 caracteres). */
public final class ClavesTemporales {

    private static final String MAY = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String MIN = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUM = "23456789";
    private static final String TODOS = MAY + MIN + NUM;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClavesTemporales() {
    }

    public static String generar() {
        char[] c = new char[12];
        c[0] = MAY.charAt(RANDOM.nextInt(MAY.length()));
        c[1] = MIN.charAt(RANDOM.nextInt(MIN.length()));
        c[2] = NUM.charAt(RANDOM.nextInt(NUM.length()));
        for (int i = 3; i < c.length; i++) {
            c[i] = TODOS.charAt(RANDOM.nextInt(TODOS.length()));
        }
        // Mezcla (Fisher-Yates) para que la mayuscula no quede siempre de primera
        for (int i = c.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        return new String(c);
    }
}
