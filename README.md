# ALMORA Donante – App móvil (Flutter)

App móvil del **Banco de Alimentos**, solo para **donantes**. Consume la misma API REST del backend
Spring Boot (`almora-api`), así que todo lo que se hace en la app aparece en la versión web y al revés.

## Casos de uso implementados

| CU | Nombre | Pantalla |
|----|--------|----------|
| CU01 | Iniciar sesión (incluye bloqueo tras 5 intentos fallidos) | `acceso_screen.dart` (pestaña Ingresar) |
| CU02 | Registrarse como donante | `acceso_screen.dart` (pestaña Crear cuenta) |
| CU03 | Registrar donación (y editarla si está pendiente) | `nueva_donacion_screen.dart` |
| CU04 | Rastrear donación (+ descargar certificado) | `detalle_donacion_screen.dart` |
| CU05 | Anular donación pendiente | `detalle_donacion_screen.dart` |
| **CU06** | **Consultar mis donaciones** (principal: filtrar, cargar más, actualizar) | `inicio_screen.dart` |

## Estructura

```
lib/
├── main.dart                 Punto de entrada
├── config/                   URL del backend y colores de la marca
├── models/                   Clases que representan el JSON de la API
├── services/                 Llamadas HTTP (una clase por grupo de endpoints)
│   └── api_client.dart       Envía el token, renueva la sesión y traduce errores
├── utils/                    Formato de fechas, estados y unidades
├── widgets/                  Piezas visuales reutilizables
└── screens/                  Las pantallas de la app
```

## Cómo funciona la sesión

1. `POST /api/auth/login` devuelve un **token de acceso** (15 min) y una cookie `almora_refresh`.
2. El token de acceso se guarda **solo en memoria**; el valor de la cookie se guarda **cifrado** con `flutter_secure_storage`.
3. Si una petición responde **401**, la app llama `POST /api/auth/refresh` enviando la cookie y repite la petición.
4. Al abrir la app, la pantalla de carga intenta renovar la sesión; si lo logra, entra directo a Inicio.

No hace falta cambiar nada del backend.

## Configuración rápida

1. `lib/config/api_config.dart` → `urlBase`:
   - Emulador Android: `http://10.0.2.2:8080`
   - Celular real: `http://IP_DE_TU_PC:8080` (misma red Wi‑Fi)
2. `android/app/src/main/AndroidManifest.xml` → permiso de internet y `usesCleartextTraffic` (ver `android_config/AndroidManifest_REFERENCIA.xml`).
3. `flutter pub get` y `flutter run`.

## Comandos

```bash
flutter pub get        # descarga dependencias
flutter run            # ejecuta en el emulador o celular conectado
flutter test           # pruebas unitarias
flutter build apk      # genera el instalable: build/app/outputs/flutter-apk/app-release.apk
```

## Usuarios de prueba (datos demo del backend)

`donaciones@elahorro.co`, `contacto@panaderiaelsol.co`, `maria.gomez@correo.co` — contraseña `Donante2026`.
Los administradores **no** pueden entrar a la app (usan la web).
