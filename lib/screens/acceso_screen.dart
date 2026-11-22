import 'package:flutter/material.dart';

import '../config/tema.dart';
import '../services/auth_service.dart';
import '../utils/formatos.dart';
import '../widgets/mensajes.dart';
import 'inicio_screen.dart';

/// Pantalla 1 - Acceso. Tiene dos pestañas:
///  - "Ingresar"     -> CU01 Iniciar sesion
///  - "Crear cuenta" -> CU02 Registrarse como donante
class AccesoScreen extends StatefulWidget {
  const AccesoScreen({super.key});

  @override
  State<AccesoScreen> createState() => _AccesoScreenState();
}

class _AccesoScreenState extends State<AccesoScreen> with SingleTickerProviderStateMixin {
  late final TabController _pestanas;
  final AuthService _auth = AuthService();

  // ---------- formulario de ingreso
  final _formIngreso = GlobalKey<FormState>();
  final _correo = TextEditingController();
  final _password = TextEditingController();
  bool _verPassword = false;

  // ---------- formulario de registro
  final _formRegistro = GlobalKey<FormState>();
  final _rNombre = TextEditingController();
  final _rIdentificacion = TextEditingController();
  final _rCorreo = TextEditingController();
  final _rTelefono = TextEditingController();
  final _rDireccion = TextEditingController();
  final _rCiudad = TextEditingController(text: 'Cartagena');
  final _rPassword = TextEditingController();
  String _rTipo = 'PERSONA_NATURAL';

  bool _cargando = false;

  @override
  void initState() {
    super.initState();
    _pestanas = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _pestanas.dispose();
    for (final c in [_correo, _password, _rNombre, _rIdentificacion, _rCorreo, _rTelefono, _rDireccion, _rCiudad, _rPassword]) {
      c.dispose();
    }
    super.dispose();
  }

  // ======================================================================= CU01
  Future<void> _ingresar() async {
    if (!_formIngreso.currentState!.validate()) return;
    setState(() => _cargando = true);
    try {
      await _auth.iniciarSesion(_correo.text, _password.text);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const InicioScreen()));
    } catch (e) {
      // Aqui llegan: credenciales incorrectas, cuenta bloqueada por 5 intentos, sin conexion...
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  // ======================================================================= CU02
  Future<void> _registrarse() async {
    if (!_formRegistro.currentState!.validate()) return;
    setState(() => _cargando = true);
    try {
      await _auth.registrarse(
        nombre: _rNombre.text,
        tipoDonante: _rTipo,
        identificacion: _rIdentificacion.text,
        correo: _rCorreo.text,
        telefono: _rTelefono.text,
        direccion: _rDireccion.text,
        ciudad: _rCiudad.text,
        password: _rPassword.text,
      );
      if (!mounted) return;
      // Se pasa a la pestaña de ingreso con el correo ya escrito
      _correo.text = _rCorreo.text.trim();
      _password.clear();
      _pestanas.animateTo(0);
      Mensajes.exito(context, 'Cuenta creada. Ya puedes iniciar sesión.');
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  // ======================================================================= validaciones
  String? _obligatorio(String? v) => (v == null || v.trim().isEmpty) ? 'Este campo es obligatorio' : null;

  String? _validarCorreo(String? v) {
    if (v == null || v.trim().isEmpty) return 'Escribe tu correo';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v.trim())) return 'Correo inválido';
    return null;
  }

  /// Misma regla del backend: 8 a 72 caracteres, con mayuscula, minuscula y numero.
  String? _validarClaveNueva(String? v) {
    if (v == null || v.isEmpty) return 'Escribe una contraseña';
    if (!RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$').hasMatch(v)) {
      return 'Mínimo 8 caracteres, con mayúscula, minúscula y número';
    }
    return null;
  }

  // ======================================================================= vista
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _encabezado(),
            TabBar(
              controller: _pestanas,
              labelColor: Colores.institucional,
              indicatorColor: Colores.esmeralda,
              tabs: const [Tab(text: 'Ingresar'), Tab(text: 'Crear cuenta')],
            ),
            Expanded(
              child: TabBarView(
                controller: _pestanas,
                children: [_vistaIngreso(), _vistaRegistro()],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _encabezado() {
    return Container(
      width: double.infinity,
      color: Colores.institucional,
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 24),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
            child: Image.asset('assets/img/isotipo.png', width: 48, height: 48),
          ),
          const SizedBox(width: 14),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Banco de Alimentos',
                    style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                Text('Cada donación cuenta', style: TextStyle(color: Colors.white70)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _vistaIngreso() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formIngreso,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Iniciar sesión',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colores.institucional)),
            const SizedBox(height: 4),
            const Text('Ingresa con tu correo y contraseña.', style: TextStyle(color: Colores.textoSuave)),
            const SizedBox(height: 20),
            TextFormField(
              controller: _correo,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Correo', prefixIcon: Icon(Icons.email_outlined)),
              validator: _validarCorreo,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _password,
              obscureText: !_verPassword,
              decoration: InputDecoration(
                labelText: 'Contraseña',
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(_verPassword ? Icons.visibility_off : Icons.visibility),
                  onPressed: () => setState(() => _verPassword = !_verPassword),
                ),
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Escribe tu contraseña' : null,
              onFieldSubmitted: (_) => _ingresar(),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _cargando ? null : _ingresar,
              child: _cargando
                  ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Ingresar'),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: () => _pestanas.animateTo(1),
              child: const Text('¿Quieres donar? Crea tu cuenta de donante'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _vistaRegistro() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formRegistro,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Registro de donante',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colores.institucional)),
            const SizedBox(height: 4),
            const Text('Crea tu cuenta para registrar donaciones y seguir su ruta.',
                style: TextStyle(color: Colores.textoSuave)),
            const SizedBox(height: 20),
            TextFormField(
              controller: _rNombre,
              decoration: const InputDecoration(labelText: 'Nombre o razón social *'),
              validator: _obligatorio,
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _rTipo,
              decoration: const InputDecoration(labelText: 'Tipo de donante *'),
              items: Formatos.tiposDonante.entries
                  .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                  .toList(),
              onChanged: (v) => setState(() => _rTipo = v ?? 'PERSONA_NATURAL'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rIdentificacion,
              decoration: const InputDecoration(labelText: 'Cédula o NIT'),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return null; // es opcional
                return RegExp(r'^[0-9A-Za-z.-]{4,20}$').hasMatch(v.trim()) ? null : 'Identificación inválida';
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rCorreo,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Correo *'),
              validator: _validarCorreo,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rTelefono,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Teléfono *'),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Este campo es obligatorio';
                return RegExp(r'^[0-9+()\s-]{7,20}$').hasMatch(v.trim()) ? null : 'Teléfono inválido';
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rDireccion,
              decoration: const InputDecoration(labelText: 'Dirección *'),
              validator: _obligatorio,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rCiudad,
              decoration: const InputDecoration(labelText: 'Ciudad'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _rPassword,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Contraseña *',
                helperText: 'Mínimo 8 caracteres, con mayúscula, minúscula y número',
              ),
              validator: _validarClaveNueva,
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _cargando ? null : _registrarse,
              child: _cargando
                  ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Crear cuenta'),
            ),
          ],
        ),
      ),
    );
  }
}
