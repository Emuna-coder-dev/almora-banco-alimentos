import 'dart:typed_data';

import '../models/donacion.dart';
import '../models/impacto.dart';
import '../models/seguimiento.dart';
import 'api_client.dart';

/// Un producto que el donante agrega en el formulario de nueva donacion.
class ItemFormulario {
  String? productoId;
  int cantidad;
  DateTime? fechaVencimiento;

  ItemFormulario({this.productoId, this.cantidad = 1, this.fechaVencimiento});

  /// Formato que espera el backend: {"productoId": "...", "cantidad": 10, "fechaVencimiento": "2026-12-31"}
  Map<String, dynamic> toJson() => {
        'productoId': productoId,
        'cantidad': cantidad,
        'fechaVencimiento': fechaVencimiento == null ? null : _fechaIso(fechaVencimiento!),
      };

  static String _fechaIso(DateTime f) =>
      '${f.year.toString().padLeft(4, '0')}-${f.month.toString().padLeft(2, '0')}-${f.day.toString().padLeft(2, '0')}';
}

/// Casos de uso CU03, CU04, CU05 y CU06 (todo lo relacionado con donaciones).
class DonacionService {
  final ApiClient _api = ApiClient.instancia;

  /// CU06 - Indicadores de impacto del donante (GET /api/perfil/impacto).
  Future<Impacto> obtenerImpacto() async {
    final json = await _api.get('/api/perfil/impacto');
    return Impacto.fromJson(json as Map<String, dynamic>);
  }

  /// CU06 - Mis donaciones, paginadas y con filtro opcional por estado (GET /api/donaciones).
  /// El backend devuelve SOLO las donaciones del donante que inicio sesion.
  Future<PaginaDonaciones> listar({int pagina = 0, String? estado}) async {
    final parametros = <String, String>{'pagina': '$pagina', 'tamano': '10'};
    if (estado != null) parametros['estado'] = estado;
    final json = await _api.get('/api/donaciones', parametros: parametros);
    return PaginaDonaciones.fromJson(json as Map<String, dynamic>);
  }

  /// Detalle de una donacion (GET /api/donaciones/{id}).
  Future<Donacion> obtener(String id) async {
    final json = await _api.get('/api/donaciones/$id');
    return Donacion.fromJson(json as Map<String, dynamic>);
  }

  /// CU03 - Registrar donacion (POST /api/donaciones). Queda PENDIENTE.
  Future<Donacion> registrar(List<ItemFormulario> items, String observaciones) async {
    final json = await _api.post('/api/donaciones', cuerpo: {
      'items': items.map((i) => i.toJson()).toList(),
      'observaciones': observaciones.trim().isEmpty ? null : observaciones.trim(),
    });
    return Donacion.fromJson(json as Map<String, dynamic>);
  }

  /// CU03 (extends) - Editar donacion pendiente (PUT /api/donaciones/{id}).
  Future<Donacion> editar(String id, List<ItemFormulario> items, String observaciones) async {
    final json = await _api.put('/api/donaciones/$id', cuerpo: {
      'items': items.map((i) => i.toJson()).toList(),
      'observaciones': observaciones.trim().isEmpty ? null : observaciones.trim(),
    });
    return Donacion.fromJson(json as Map<String, dynamic>);
  }

  /// CU04 - Seguimiento tipo guia (GET /api/donaciones/{id}/seguimiento).
  Future<Seguimiento> seguimiento(String id) async {
    final json = await _api.get('/api/donaciones/$id/seguimiento');
    return Seguimiento.fromJson(json as Map<String, dynamic>);
  }

  /// CU04 (extends) - Descargar certificado PDF (GET /api/donaciones/{id}/certificado).
  Future<Uint8List> descargarCertificado(String id) => _api.descargar('/api/donaciones/$id/certificado');

  /// CU05 - Anular donacion pendiente (POST /api/donaciones/{id}/anular).
  Future<void> anular(String id, String motivo) async {
    await _api.post('/api/donaciones/$id/anular', cuerpo: {'motivo': motivo.trim()});
  }
}
