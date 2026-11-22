/// Seguimiento tipo guia de mensajeria (viene de SeguimientoResponse).
class Seguimiento {
  final String codigo;
  final String estadoActual;
  final String estadoTitulo;
  final String estadoDescripcion;
  final Progreso progreso;
  final List<PasoRuta> ruta;
  final List<EventoSeguimiento> eventos;
  final List<Destino> destinos;

  Seguimiento({
    required this.codigo,
    required this.estadoActual,
    required this.estadoTitulo,
    required this.estadoDescripcion,
    required this.progreso,
    required this.ruta,
    required this.eventos,
    required this.destinos,
  });

  factory Seguimiento.fromJson(Map<String, dynamic> json) {
    List<dynamic> lista(String campo) => (json[campo] as List<dynamic>? ?? []);
    return Seguimiento(
      codigo: json['codigo'] ?? '',
      estadoActual: json['estadoActual'] ?? '',
      estadoTitulo: json['estadoTitulo'] ?? '',
      estadoDescripcion: json['estadoDescripcion'] ?? '',
      progreso: Progreso.fromJson(json['progreso'] as Map<String, dynamic>? ?? {}),
      ruta: lista('ruta').map((e) => PasoRuta.fromJson(e as Map<String, dynamic>)).toList(),
      eventos: lista('eventos').map((e) => EventoSeguimiento.fromJson(e as Map<String, dynamic>)).toList(),
      destinos: lista('destinos').map((e) => Destino.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }
}

/// Donde estan las unidades de la donacion.
class Progreso {
  final int total;
  final int enBodega;
  final int asignadas;
  final int enRuta;
  final int entregadas;
  final int vencidas;
  final int anuladas;
  final int porcentajeEntregado;

  Progreso({
    required this.total,
    required this.enBodega,
    required this.asignadas,
    required this.enRuta,
    required this.entregadas,
    required this.vencidas,
    required this.anuladas,
    required this.porcentajeEntregado,
  });

  factory Progreso.fromJson(Map<String, dynamic> json) {
    int n(String campo) => (json[campo] as num?)?.toInt() ?? 0;
    return Progreso(
      total: n('total'),
      enBodega: n('enBodega'),
      asignadas: n('asignadas'),
      enRuta: n('enRuta'),
      entregadas: n('entregadas'),
      vencidas: n('vencidas'),
      anuladas: n('anuladas'),
      porcentajeEntregado: n('porcentajeEntregado'),
    );
  }
}

/// Un paso de la ruta: Registrada, Recibida, Almacenada...
class PasoRuta {
  final String estado;
  final String titulo;
  final bool completado;
  final bool actual;
  final String? fecha;

  PasoRuta({
    required this.estado,
    required this.titulo,
    required this.completado,
    required this.actual,
    this.fecha,
  });

  factory PasoRuta.fromJson(Map<String, dynamic> json) {
    return PasoRuta(
      estado: json['estado'] ?? '',
      titulo: json['titulo'] ?? '',
      completado: json['completado'] ?? false,
      actual: json['actual'] ?? false,
      fecha: json['fecha'],
    );
  }
}

/// Un evento del historial de la donacion.
class EventoSeguimiento {
  final String titulo;
  final String? descripcion;
  final String? fecha;
  final String? ubicacion;
  final String? responsable;

  EventoSeguimiento({required this.titulo, this.descripcion, this.fecha, this.ubicacion, this.responsable});

  factory EventoSeguimiento.fromJson(Map<String, dynamic> json) {
    return EventoSeguimiento(
      titulo: json['titulo'] ?? '',
      descripcion: json['descripcion'],
      fecha: json['fecha'],
      ubicacion: json['ubicacion'],
      responsable: json['responsable'],
    );
  }
}

/// Fundacion a la que llego (o va a llegar) parte de la donacion.
class Destino {
  final String fundacion;
  final String estadoTitulo;
  final int unidades;
  final String? fechaEntrega;
  final String? fechaProgramada;
  final String? receptor;

  Destino({
    required this.fundacion,
    required this.estadoTitulo,
    required this.unidades,
    this.fechaEntrega,
    this.fechaProgramada,
    this.receptor,
  });

  factory Destino.fromJson(Map<String, dynamic> json) {
    return Destino(
      fundacion: json['fundacion'] ?? '',
      estadoTitulo: json['estadoTitulo'] ?? '',
      unidades: (json['unidades'] as num?)?.toInt() ?? 0,
      fechaEntrega: json['fechaEntrega'],
      fechaProgramada: json['fechaProgramada'],
      receptor: json['receptor'],
    );
  }
}
