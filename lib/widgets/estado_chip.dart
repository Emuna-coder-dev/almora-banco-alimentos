import 'package:flutter/material.dart';

import '../utils/formatos.dart';

/// Etiqueta de color con el estado de una donacion (ej: "Entregada" en verde).
class EstadoChip extends StatelessWidget {
  final String estado;
  final String titulo;

  const EstadoChip({super.key, required this.estado, required this.titulo});

  @override
  Widget build(BuildContext context) {
    final color = Formatos.colorEstado(estado);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        titulo,
        style: TextStyle(color: color, fontWeight: FontWeight.w600, fontSize: 12),
      ),
    );
  }
}
