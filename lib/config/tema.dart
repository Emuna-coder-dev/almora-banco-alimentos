import 'package:flutter/material.dart';

/// Colores del manual de marca (los mismos de la version web).
class Colores {
  static const Color institucional = Color(0xFF1B365D); // titulos, barra superior
  static const Color esmeralda = Color(0xFF10B981); // botones y acentos
  static const Color esmeraldaOscuro = Color(0xFF059669);
  static const Color dinamico = Color(0xFF3B82F6); // enlaces y progreso
  static const Color fondo = Color(0xFFF5F7FA);
  static const Color borde = Color(0xFFE2E8F0);
  static const Color texto = Color(0xFF1B2B44);
  static const Color textoSuave = Color(0xFF4A5873);
  static const Color peligro = Color(0xFFDC2626);
  static const Color advertencia = Color(0xFFF59E0B);
}

/// Tema visual de toda la aplicacion.
ThemeData crearTema() {
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colores.esmeralda,
      primary: Colores.esmeralda,
      secondary: Colores.dinamico,
      surface: Colors.white,
    ),
    scaffoldBackgroundColor: Colores.fondo,
  );

  return base.copyWith(
    appBarTheme: const AppBarTheme(
      backgroundColor: Colores.institucional,
      foregroundColor: Colors.white,
      centerTitle: false,
      elevation: 0,
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: Colores.esmeralda,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(48),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: Colores.institucional,
        minimumSize: const Size.fromHeight(46),
        side: const BorderSide(color: Colores.borde),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: Colores.borde),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: Colores.borde),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: Colores.esmeralda, width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Colores.esmeralda,
      foregroundColor: Colors.white,
    ),
  );
}
