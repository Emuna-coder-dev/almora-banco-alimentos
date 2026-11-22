import '../models/producto.dart';
import 'api_client.dart';

/// Catalogo de productos (se usa en CU03 - Registrar donacion).
class ProductoService {
  final ApiClient _api = ApiClient.instancia;

  /// GET /api/productos: devuelve solo los productos activos, ordenados por nombre.
  Future<List<Producto>> listar() async {
    final json = await _api.get('/api/productos');
    final lista = (json as List<dynamic>).map((e) => Producto.fromJson(e as Map<String, dynamic>)).toList();
    lista.sort((a, b) => a.nombre.toLowerCase().compareTo(b.nombre.toLowerCase()));
    return lista;
  }
}
