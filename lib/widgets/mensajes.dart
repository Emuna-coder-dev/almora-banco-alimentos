import 'package:flutter/material.dart';

import '../config/tema.dart';

/// Mensajes cortos en la parte inferior de la pantalla (SnackBar).
class Mensajes {
  static void error(BuildContext context, Object error) {
    _mostrar(context, error.toString(), Colores.peligro);
  }

  static void exito(BuildContext context, String texto) {
    _mostrar(context, texto, Colores.esmeraldaOscuro);
  }

  static void _mostrar(BuildContext context, String texto, Color color) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(texto),
        backgroundColor: color,
        behavior: SnackBarBehavior.floating,
      ));
  }
}

/// Bloque que se muestra cuando una pantalla no pudo cargar sus datos.
class VistaError extends StatelessWidget {
  final String mensaje;
  final VoidCallback alReintentar;

  const VistaError({super.key, required this.mensaje, required this.alReintentar});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.wifi_off, size: 48, color: Colores.textoSuave),
            const SizedBox(height: 12),
            Text(mensaje, textAlign: TextAlign.center, style: const TextStyle(color: Colores.textoSuave)),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: alReintentar,
              icon: const Icon(Icons.refresh),
              label: const Text('Reintentar'),
            ),
          ],
        ),
      ),
    );
  }
}
