import '../models/usuario.dart';
import 'api_client.dart';

/// Casos de uso CU01 (Iniciar sesion) y CU02 (Registrarse como donante).
class AuthService {
  final ApiClient _api = ApiClient.instancia;

  /// CU01 - Iniciar sesion. Solo los donantes pueden usar la app movil.
  Future<Usuario> iniciarSesion(String correo, String password) async {
    final usuario = await _api.iniciarSesion(correo.trim(), password);
    if (!usuario.esDonante) {
      // Los administradores trabajan en la version web
      await _api.cerrarSesion();
      throw ApiException(403, 'Esta aplicación es solo para donantes. Los administradores usan la versión web.');
    }
    return usuario;
  }

  /// CU02 - Registrarse como donante (POST /api/auth/registro).
  Future<void> registrarse({
    required String nombre,
    required String tipoDonante,
    required String identificacion,
    required String correo,
    required String telefono,
    required String direccion,
    required String ciudad,
    required String password,
  }) async {
    await _api.post('/api/auth/registro', cuerpo: {
      'nombre': nombre.trim(),
      'tipoDonante': tipoDonante,
      // La identificacion es opcional: si va vacia se manda null
      'identificacion': identificacion.trim().isEmpty ? null : identificacion.trim(),
      'correo': correo.trim(),
      'telefono': telefono.trim(),
      'direccion': direccion.trim(),
      'ciudad': ciudad.trim().isEmpty ? null : ciudad.trim(),
      'password': password,
    });
  }

  /// Intenta recuperar la sesion guardada (se usa al abrir la app).
  Future<bool> recuperarSesion() async {
    final ok = await _api.renovarSesion();
    return ok && (_api.usuario?.esDonante ?? false);
  }

  Future<void> cerrarSesion() => _api.cerrarSesion();

  Usuario? get usuarioActual => _api.usuario;
}
