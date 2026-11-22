import 'package:flutter/material.dart';

import 'config/tema.dart';
import 'screens/splash_screen.dart';

/// Punto de entrada de la app ALMORA Donante.
void main() {
  runApp(const AlmoraApp());
}

class AlmoraApp extends StatelessWidget {
  const AlmoraApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ALMORA Donante',
      debugShowCheckedModeBanner: false,
      theme: crearTema(),
      // La primera pantalla revisa si hay una sesion guardada
      home: const SplashScreen(),
    );
  }
}
