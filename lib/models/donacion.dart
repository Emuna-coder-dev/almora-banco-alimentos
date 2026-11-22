/// Producto dentro de una donacion (viene de ItemDonacionResponse).
class ItemDonacion {
  final String productoId;
  final String nombreProducto;
  final String categoria;
  final bool perecedero;
  final String unidadMedida;
  final int cantidad;
  final String? fechaVencimiento; // formato "2026-12-31" o null

  ItemDonacion({
    required this.productoId,
    required this.nombreProducto,
    required this.categoria,
    required this.perecedero,
    required this.unidadMedida,
    required this.cantidad,
    this.fechaVencimiento,
  });

  factory ItemDonacion.fromJson(Map<String, dynamic> json) {
    return ItemDonacion(
      productoId: json['productoId'] ?? '',
      nombreProducto: json['nombreProducto'] ?? '',
      categoria: json['categoria'] ?? 'OTRO',
      perecedero: json['perecedero'] ?? false,
      unidadMedida: json['unidadMedida'] ?? 'UNIDAD',
      cantidad: (json['cantidad'] as num?)?.toInt() ?? 0,
      fechaVencimiento: json['fechaVencimiento'],
    );
  }
}

/// Una donacion (viene de DonacionResponse).
class Donacion {
  final String id;
  final String codigo;
  final List<ItemDonacion> items;
  final int totalUnidades;
  final String estado; // PENDIENTE, RECIBIDA, ...
  final String estadoTitulo;
  final String? fechaRegistro;
  final String? observaciones;
  final bool certificadoDisponible;

  Donacion({
    required this.id,
    required this.codigo,
    required this.items,
    required this.totalUnidades,
    required this.estado,
    required this.estadoTitulo,
    this.fechaRegistro,
    this.observaciones,
    this.certificadoDisponible = false,
  });

  factory Donacion.fromJson(Map<String, dynamic> json) {
    final lista = (json['items'] as List<dynamic>? ?? []);
    return Donacion(
      id: json['id'] ?? '',
      codigo: json['codigo'] ?? '',
      items: lista.map((e) => ItemDonacion.fromJson(e as Map<String, dynamic>)).toList(),
      totalUnidades: (json['totalUnidades'] as num?)?.toInt() ?? 0,
      estado: json['estado'] ?? '',
      estadoTitulo: json['estadoTitulo'] ?? '',
      fechaRegistro: json['fechaRegistro'],
      observaciones: json['observaciones'],
      certificadoDisponible: json['certificadoDisponible'] ?? false,
    );
  }

  /// Solo se puede editar o anular mientras el banco no la haya recibido.
  bool get esPendiente => estado == 'PENDIENTE';
}

/// Una pagina de donaciones (viene de Pagina<DonacionResponse>).
class PaginaDonaciones {
  final List<Donacion> contenido;
  final int pagina;
  final int totalPaginas;
  final int totalElementos;

  PaginaDonaciones({
    required this.contenido,
    required this.pagina,
    required this.totalPaginas,
    required this.totalElementos,
  });

  factory PaginaDonaciones.fromJson(Map<String, dynamic> json) {
    final lista = (json['contenido'] as List<dynamic>? ?? []);
    return PaginaDonaciones(
      contenido: lista.map((e) => Donacion.fromJson(e as Map<String, dynamic>)).toList(),
      pagina: (json['pagina'] as num?)?.toInt() ?? 0,
      totalPaginas: (json['totalPaginas'] as num?)?.toInt() ?? 0,
      totalElementos: (json['totalElementos'] as num?)?.toInt() ?? 0,
    );
  }

  /// true si todavia quedan paginas por cargar.
  bool get hayMas => pagina + 1 < totalPaginas;
}
