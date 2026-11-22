/// Datos del usuario que inicio sesion (viene de UsuarioResponse del backend).
class Usuario {
  final String id;
  final String nombre;
  final String correo;
  final String rol; // SUPERADMIN, ADMIN o DONANTE
  final String? telefono;
  final bool debeCambiarPassword;
  final String? tipoDonante;
  final String? identificacion;
  final String? direccion;
  final String? ciudad;

  Usuario({
    required this.id,
    required this.nombre,
    required this.correo,
    required this.rol,
    this.telefono,
    this.debeCambiarPassword = false,
    this.tipoDonante,
    this.identificacion,
    this.direccion,
    this.ciudad,
  });

  /// Convierte el JSON que responde la API en un objeto Usuario.
  factory Usuario.fromJson(Map<String, dynamic> json) {
    return Usuario(
      id: json['id'] ?? '',
      nombre: json['nombre'] ?? '',
      correo: json['correo'] ?? '',
      rol: json['rol'] ?? '',
      telefono: json['telefono'],
      debeCambiarPassword: json['debeCambiarPassword'] ?? false,
      tipoDonante: json['tipoDonante'],
      identificacion: json['identificacion'],
      direccion: json['direccion'],
      ciudad: json['ciudad'],
    );
  }

  bool get esDonante => rol == 'DONANTE';

  /// Primer nombre, para saludar: "Maria Gomez" -> "Maria".
  String get primerNombre => nombre.trim().split(' ').first;
}
