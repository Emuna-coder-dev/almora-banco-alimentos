import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/usuario.dart';

/// Error que devuelve la API (o la falta de conexion), con un mensaje listo para mostrar.
class ApiException implements Exception {
  final int status; // 0 = no hubo conexion con el servidor
  final String mensaje;

  ApiException(this.status, this.mensaje);

  @override
  String toString() => mensaje;
}

/// Cliente HTTP de toda la app. Hace tres cosas:
///  1. Envia el token JWT en el encabezado Authorization de cada peticion.
///  2. Si el token vencio (respuesta 401), renueva la sesion con el token de refresco y repite la peticion.
///  3. Convierte los errores del backend (ErrorResponse) en un ApiException con un mensaje claro.
///
/// Es un "singleton": hay una sola instancia para toda la app (ApiClient.instancia).
class ApiClient {
  ApiClient._();
  static final ApiClient instancia = ApiClient._();

  static const String _claveCookie = 'almora_refresh';
  final FlutterSecureStorage _almacen = const FlutterSecureStorage();

  /// Token de acceso JWT (dura 15 minutos). Solo vive en memoria, nunca se guarda en disco.
  String? _token;

  /// Usuario que inicio sesion.
  Usuario? usuario;

  /// Si ya hay una renovacion en curso, las demas peticiones la esperan (no se renueva dos veces).
  Future<bool>? _renovando;

  bool get haySesion => _token != null && usuario != null;

  // ======================================================================= peticiones

  Future<dynamic> get(String ruta, {Map<String, String>? parametros}) async {
    final r = await _enviar('GET', ruta, parametros: parametros);
    return _leerJson(r);
  }

  Future<dynamic> post(String ruta, {Object? cuerpo}) async {
    final r = await _enviar('POST', ruta, cuerpo: cuerpo);
    return _leerJson(r);
  }

  Future<dynamic> put(String ruta, {Object? cuerpo}) async {
    final r = await _enviar('PUT', ruta, cuerpo: cuerpo);
    return _leerJson(r);
  }

  /// Descarga un archivo (por ejemplo, el certificado PDF) y devuelve sus bytes.
  Future<Uint8List> descargar(String ruta) async {
    final r = await _enviar('GET', ruta, aceptar: 'application/pdf');
    return r.bodyBytes;
  }

  Future<http.Response> _enviar(
    String metodo,
    String ruta, {
    Object? cuerpo,
    Map<String, String>? parametros,
    String aceptar = 'application/json',
    bool reintentar = true,
  }) async {
    var uri = Uri.parse('${ApiConfig.urlBase}$ruta');
    if (parametros != null && parametros.isNotEmpty) {
      uri = uri.replace(queryParameters: parametros);
    }

    final encabezados = <String, String>{
      'Content-Type': 'application/json',
      'Accept': aceptar,
    };
    if (_token != null) {
      encabezados['Authorization'] = 'Bearer $_token';
    }

    http.Response respuesta;
    try {
      final texto = cuerpo == null ? null : jsonEncode(cuerpo);
      switch (metodo) {
        case 'POST':
          respuesta = await http.post(uri, headers: encabezados, body: texto).timeout(ApiConfig.tiempoEspera);
          break;
        case 'PUT':
          respuesta = await http.put(uri, headers: encabezados, body: texto).timeout(ApiConfig.tiempoEspera);
          break;
        default:
          respuesta = await http.get(uri, headers: encabezados).timeout(ApiConfig.tiempoEspera);
      }
    } on TimeoutException {
      throw ApiException(0, 'El servidor tardó demasiado en responder. Intenta de nuevo.');
    } on SocketException {
      throw ApiException(0, 'No se pudo conectar con el servidor. Revisa tu conexión y que el backend esté encendido.');
    } on http.ClientException {
      throw ApiException(0, 'No se pudo conectar con el servidor. Revisa tu conexión y que el backend esté encendido.');
    }

    // 401 = el token vencio. Se intenta renovar UNA vez y se repite la misma peticion.
    if (respuesta.statusCode == 401 && reintentar && !ruta.startsWith('/api/auth/')) {
      final renovada = await renovarSesion();
      if (renovada) {
        return _enviar(metodo, ruta,
            cuerpo: cuerpo, parametros: parametros, aceptar: aceptar, reintentar: false);
      }
      throw ApiException(401, 'Tu sesión terminó. Vuelve a iniciar sesión.');
    }

    if (respuesta.statusCode >= 400) {
      throw _crearError(respuesta);
    }
    return respuesta;
  }

  dynamic _leerJson(http.Response r) {
    if (r.bodyBytes.isEmpty) return null;
    // utf8.decode para que las tildes y la ñ se vean bien
    return jsonDecode(utf8.decode(r.bodyBytes));
  }

  /// Lee el JSON de error del backend: { "status": 409, "mensaje": "...", "detalles": {...} }
  ApiException _crearError(http.Response r) {
    String mensaje = 'Ocurrió un error (código ${r.statusCode}).';
    try {
      final json = jsonDecode(utf8.decode(r.bodyBytes));
      if (json is Map<String, dynamic>) {
        if (json['mensaje'] != null) mensaje = json['mensaje'].toString();
        // Errores de validacion por campo: se muestra el primero
        final detalles = json['detalles'];
        if (detalles is Map && detalles.isNotEmpty) {
          mensaje = detalles.values.first.toString();
        }
      }
    } catch (_) {
      // la respuesta no era JSON: se deja el mensaje generico
    }
    return ApiException(r.statusCode, mensaje);
  }

  // ======================================================================= sesion

  /// Inicia sesion con correo y contraseña. Devuelve el usuario.
  Future<Usuario> iniciarSesion(String correo, String password) async {
    final r = await _enviar('POST', '/api/auth/login',
        cuerpo: {'correo': correo, 'password': password}, reintentar: false);
    await _guardarSesion(r);
    return usuario!;
  }

  /// Renueva el token de acceso usando el token de refresco guardado.
  /// Devuelve true si se pudo renovar.
  Future<bool> renovarSesion() {
    // Si ya hay una renovacion en curso, se reutiliza
    _renovando ??= _renovar().whenComplete(() => _renovando = null);
    return _renovando!;
  }

  Future<bool> _renovar() async {
    final cookie = await _almacen.read(key: _claveCookie);
    if (cookie == null || cookie.isEmpty) return false;
    try {
      final r = await http
          .post(
            Uri.parse('${ApiConfig.urlBase}/api/auth/refresh'),
            headers: {
              'Accept': 'application/json',
              // El backend espera el token de refresco en una cookie, igual que un navegador
              'Cookie': '${ApiConfig.cookieRefresco}=$cookie',
            },
          )
          .timeout(ApiConfig.tiempoEspera);
      if (r.statusCode == 200) {
        await _guardarSesion(r);
        return true;
      }
      await _borrarSesion();
      return false;
    } catch (_) {
      return false; // sin conexion: no se borra la sesion, se podra intentar despues
    }
  }

  /// Cierra la sesion en el servidor y en el celular.
  Future<void> cerrarSesion() async {
    final cookie = await _almacen.read(key: _claveCookie);
    try {
      if (cookie != null) {
        await http.post(
          Uri.parse('${ApiConfig.urlBase}/api/auth/logout'),
          headers: {'Cookie': '${ApiConfig.cookieRefresco}=$cookie'},
        ).timeout(ApiConfig.tiempoEspera);
      }
    } catch (_) {
      // Si no hay conexion, igual se cierra la sesion en el celular
    }
    await _borrarSesion();
  }

  /// Guarda el token de acceso (en memoria) y el token de refresco (cifrado en el celular).
  Future<void> _guardarSesion(http.Response r) async {
    final json = jsonDecode(utf8.decode(r.bodyBytes)) as Map<String, dynamic>;
    _token = json['accessToken'];
    usuario = Usuario.fromJson(json['usuario'] as Map<String, dynamic>);

    final refresco = _leerCookie(r.headers['set-cookie']);
    if (refresco != null && refresco.isNotEmpty) {
      await _almacen.write(key: _claveCookie, value: refresco);
    }
  }

  Future<void> _borrarSesion() async {
    _token = null;
    usuario = null;
    await _almacen.delete(key: _claveCookie);
  }

  /// Saca el valor de la cookie "almora_refresh" del encabezado Set-Cookie.
  /// Ejemplo: "almora_refresh=abc123; Path=/api/auth; HttpOnly" -> "abc123"
  String? _leerCookie(String? setCookie) {
    if (setCookie == null) return null;
    final coincidencia = RegExp('${ApiConfig.cookieRefresco}=([^;,]*)').firstMatch(setCookie);
    return coincidencia?.group(1);
  }
}
