import 'package:flutter/material.dart';

import '../config/tema.dart';

/// Tarjeta pequeña con un numero y su descripcion (indicadores de impacto).
class TarjetaIndicador extends StatelessWidget {
  final IconData icono;
  final Color color;
  final String valor;
  final String titulo;

  const TarjetaIndicador({
    super.key,
    required this.icono,
    required this.color,
    required this.valor,
    required this.titulo,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colores.borde),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 20,
            backgroundColor: color.withOpacity(0.15),
            child: Icon(icono, color: color, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(valor,
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colores.texto)),
                Text(titulo,
                    style: const TextStyle(fontSize: 12, color: Colores.textoSuave),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
