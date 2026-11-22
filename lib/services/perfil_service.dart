import '../models/usuario.dart';
import 'api_client.dart';

/// Pantalla de perfil: ver y editar mis datos, y cambiar la contraseña.
class PerfilService {
  final ApiClient _api = ApiClient.instancia;

  /// GET /api/perfil
  Future<Usuario> obtener() async {
    final json = await _api.get('/api/perfil');
    return Usuario.fromJson(json as Map<String, dynamic>);
  }

  /// PUT /api/perfil
  Future<Usuario> actualizar({
    required String nombre,
    required String telefono,
    required String direccion,
    required String ciudad,
  }) async {
    final json = await _api.put('/api/perfil', cuerpo: {
      'nombre': nombre.trim(),
      // Si el telefono va vacio se manda null (el backend no acepta un telefono vacio)
      'telefono': telefono.trim().isEmpty ? null : telefono.trim(),
      'direccion': direccion.trim(),
      'ciudad': ciudad.trim(),
    });
    final usuario = Usuario.fromJson(json as Map<String, dynamic>);
    _api.usuario = usuario; // para que el saludo de Inicio use el nombre nuevo
    return usuario;
  }

  /// POST /api/perfil/password
  Future<void> cambiarPassword(String actual, String nueva) async {
    await _api.post('/api/perfil/password', cuerpo: {'actual': actual, 'nueva': nueva});
  }
}
