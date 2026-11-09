package com.almora.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Genera codigos legibles y dificiles de adivinar: DON-261013-K7P2QX, ENT-..., LOT-..., CERT-... */
public final class Codigos {

    /** Sin letras ni numeros que se confunden (0/O, 1/I/L). */
    private static final char[] ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Codigos() {
    }

    public static String generar(String prefijo) {
        return generar(prefijo, 6);
    }

    public static String generar(String prefijo, int largo) {
        StringBuilder sb = new StringBuilder(prefijo).append('-')
                .append(LocalDate.now(ZONA).format(FECHA)).append('-');
        for (int i = 0; i < largo; i++) {
            sb.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }
}
