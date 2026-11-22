/// Producto del catalogo (viene de ProductoResponse del backend).
class Producto {
  final String id;
  final String nombre;
  final String categoria;
  final bool perecedero;
  final String unidadMedida;

  Producto({
    required this.id,
    required this.nombre,
    required this.categoria,
    required this.perecedero,
    required this.unidadMedida,
  });

  factory Producto.fromJson(Map<String, dynamic> json) {
    return Producto(
      id: json['id'] ?? '',
      nombre: json['nombre'] ?? '',
      categoria: json['categoria'] ?? 'OTRO',
      perecedero: json['perecedero'] ?? false,
      unidadMedida: json['unidadMedida'] ?? 'UNIDAD',
    );
  }
}
