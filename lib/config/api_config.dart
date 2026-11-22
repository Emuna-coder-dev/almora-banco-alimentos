/// Configuracion de la conexion con el backend.
///
/// IMPORTANTE: la app corre en el celular (o en el emulador), NO en tu computador.
/// Por eso "localhost" no sirve: hay que usar la direccion del computador donde corre el backend.
///
///  - Emulador de Android Studio ......... http://10.0.2.2:8080
///    (10.0.2.2 es la forma en que el emulador llama a "localhost" de tu computador)
///  - Celular real conectado al mismo WiFi .. http://IP_DE_TU_COMPUTADOR:8080
///    (ejemplo: http://192.168.1.15:8080; la IP la ves con el comando "ipconfig")
class ApiConfig {
  /// Cambia esta linea segun donde vayas a probar la app.
    static const String urlBase = 'http://localhost:8080';

  /// Tiempo maximo de espera de cada peticion.
  static const Duration tiempoEspera = Duration(seconds: 20);

  /// Nombre de la cookie con la que el backend entrega el token de refresco.
  static const String cookieRefresco = 'almora_refresh';
}
