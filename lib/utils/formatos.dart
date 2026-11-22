import 'package:flutter/material.dart';

import '../config/tema.dart';

/// Funciones pequeñas para mostrar fechas, estados y nombres de forma amigable.
class Formatos {
  /// "2026-11-20T15:30:00Z" o "2026-11-20" -> "20/11/2026"
  static String fecha(String? iso) {
    if (iso == null || iso.isEmpty) return '-';
    final f = DateTime.tryParse(iso);
    if (f == null) return iso;
    final local = f.toLocal();
    return '${_dos(local.day)}/${_dos(local.month)}/${local.year}';
  }

  /// "2026-11-20T15:30:00Z" -> "20/11/2026 10:30"
  static String fechaHora(String? iso) {
    if (iso == null || iso.isEmpty) return '-';
    final f = DateTime.tryParse(iso);
    if (f == null) return iso;
    final local = f.toLocal();
    return '${fecha(iso)} ${_dos(local.hour)}:${_dos(local.minute)}';
  }

  /// DateTime -> "20/11/2026" (para el selector de fecha del formulario)
  static String fechaCorta(DateTime f) => '${_dos(f.day)}/${_dos(f.month)}/${f.year}';

  static String _dos(int n) => n.toString().padLeft(2, '0');

  /// Color de cada estado de la donacion (igual que en la web).
  static Color colorEstado(String estado) {
    switch (estado) {
      case 'PENDIENTE':
        return Colores.textoSuave;
      case 'RECIBIDA':
      case 'ALMACENADA':
        return Colores.dinamico;
      case 'ASIGNADA':
      case 'EN_RUTA':
        return Colores.institucional;
      case 'PARCIALMENTE_ENTREGADA':
      case 'ENTREGADA':
        return Colores.esmeraldaOscuro;
      case 'VENCIDA':
        return Colores.advertencia;
      case 'ANULADA':
        return Colores.peligro;
      default:
        return Colores.textoSuave;
    }
  }

  /// Estados que se pueden usar en el filtro de la pantalla de Inicio.
  static const Map<String, String> estados = {
    'PENDIENTE': 'Pendiente',
    'RECIBIDA': 'Recibida',
    'ALMACENADA': 'Almacenada',
    'ASIGNADA': 'Asignada',
    'EN_RUTA': 'En ruta',
    'PARCIALMENTE_ENTREGADA': 'Parcialmente entregada',
    'ENTREGADA': 'Entregada',
    'VENCIDA': 'Vencida',
    'ANULADA': 'Anulada',
  };

  /// Tipos de donante (enum TipoDonante del backend).
  static const Map<String, String> tiposDonante = {
    'PERSONA_NATURAL': 'Persona natural',
    'EMPRESA': 'Empresa',
    'SUPERMERCADO': 'Supermercado',
    'RESTAURANTE': 'Restaurante',
    'ORGANIZACION': 'Organización',
    'OTRO': 'Otro',
  };

  /// Unidades de medida (enum UnidadMedida del backend), en forma corta.
  static String unidad(String unidad) {
    switch (unidad) {
      case 'KILOGRAMO':
        return 'kg';
      case 'LIBRA':
        return 'lb';
      case 'LITRO':
        return 'L';
      case 'PAQUETE':
        return 'paq';
      case 'CAJA':
        return 'cajas';
      default:
        return 'und';
    }
  }
}
