import 'package:flutter/material.dart';

import '../config/tema.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';
import '../services/perfil_service.dart';
import '../utils/formatos.dart';
import '../widgets/mensajes.dart';
import 'acceso_screen.dart';

/// Pantalla 5 - Mi perfil: ver/editar mis datos, cambiar contraseña y cerrar sesion.
class PerfilScreen extends StatefulWidget {
  const PerfilScreen({super.key});

  @override
  State<PerfilScreen> createState() => _PerfilScreenState();
}

class _PerfilScreenState extends State<PerfilScreen> {
  final PerfilService _perfil = PerfilService();
  final AuthService _auth = AuthService();

  final _formDatos = GlobalKey<FormState>();
  final _nombre = TextEditingController();
  final _telefono = TextEditingController();
  final _direccion = TextEditingController();
  final _ciudad = TextEditingController();

  Usuario? _usuario;
  bool _cargando = true;
  bool _guardando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    for (final c in [_nombre, _telefono, _direccion, _ciudad]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final u = await _perfil.obtener();
      if (!mounted) return;
      setState(() {
        _usuario = u;
        _nombre.text = u.nombre;
        _telefono.text = u.telefono ?? '';
        _direccion.text = u.direccion ?? '';
        _ciudad.text = u.ciudad ?? '';
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  Future<void> _guardarDatos() async {
    if (!_formDatos.currentState!.validate()) return;
    setState(() => _guardando = true);
    try {
      final u = await _perfil.actualizar(
        nombre: _nombre.text,
        telefono: _telefono.text,
        direccion: _direccion.text,
        ciudad: _ciudad.text,
      );
      if (!mounted) return;
      setState(() => _usuario = u);
      if (mounted) Mensajes.exito(context, 'Datos actualizados');
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  /// Dialogo para cambiar la contraseña. El backend cierra todas las sesiones despues del cambio,
  /// por eso al terminar se vuelve a la pantalla de acceso.
  Future<void> _cambiarPassword() async {
    final actual = TextEditingController();
    final nueva = TextEditingController();
    final repetir = TextEditingController();
    final formulario = GlobalKey<FormState>();

    final confirmado = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('Cambiar contraseña'),
        content: Form(
          key: formulario,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: actual,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Contraseña actual'),
                  validator: (v) => (v == null || v.isEmpty) ? 'Obligatoria' : null,
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: nueva,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Nueva contraseña'),
                  validator: (v) => RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$').hasMatch(v ?? '')
                      ? null
                      : 'Mínimo 8, con mayúscula, minúscula y número',
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: repetir,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Repite la nueva contraseña'),
                  validator: (v) => v == nueva.text ? null : 'No coincide',
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('Cancelar')),
          TextButton(
            onPressed: () {
              if (formulario.currentState!.validate()) Navigator.pop(contexto, true);
            },
            child: const Text('Cambiar'),
          ),
        ],
      ),
    );
    if (confirmado != true) return;

    try {
      await _perfil.cambiarPassword(actual.text, nueva.text);
      await _auth.cerrarSesion();
      if (!mounted) return;
      Mensajes.exito(context, 'Contraseña actualizada. Inicia sesión de nuevo.');
      _irAAcceso();
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    }
  }

  Future<void> _cerrarSesion() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('Cerrar sesión'),
        content: const Text('¿Seguro que quieres salir?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(contexto, true), child: const Text('Salir')),
        ],
      ),
    );
    if (ok != true) return;
    await _auth.cerrarSesion();
    if (mounted) _irAAcceso();
  }

  /// Borra todas las pantallas anteriores y deja solo la de acceso.
  void _irAAcceso() {
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const AccesoScreen()),
      (_) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil')),
      body: _cargando
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? VistaError(mensaje: _error!, alReintentar: _cargar)
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    _tarjetaUsuario(),
                    const SizedBox(height: 20),
                    const Text('Mis datos',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colores.institucional)),
                    const SizedBox(height: 10),
                    Form(
                      key: _formDatos,
                      child: Column(
                        children: [
                          TextFormField(
                            controller: _nombre,
                            decoration: const InputDecoration(labelText: 'Nombre o razón social'),
                            validator: (v) => (v == null || v.trim().isEmpty) ? 'Obligatorio' : null,
                          ),
                          const SizedBox(height: 10),
                          TextFormField(
                            controller: _telefono,
                            keyboardType: TextInputType.phone,
                            decoration: const InputDecoration(labelText: 'Teléfono'),
                            validator: (v) {
                              final t = (v ?? '').trim();
                              if (t.isEmpty) return null;
                              return RegExp(r'^[0-9+()\s-]{7,20}$').hasMatch(t) ? null : 'Teléfono inválido';
                            },
                          ),
                          const SizedBox(height: 10),
                          TextFormField(
                            controller: _direccion,
                            decoration: const InputDecoration(labelText: 'Dirección'),
                          ),
                          const SizedBox(height: 10),
                          TextFormField(
                            controller: _ciudad,
                            decoration: const InputDecoration(labelText: 'Ciudad'),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: _guardando ? null : _guardarDatos,
                      child: _guardando
                          ? const SizedBox(
                              width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Text('Guardar cambios'),
                    ),
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: _cambiarPassword,
                      icon: const Icon(Icons.lock_reset),
                      label: const Text('Cambiar contraseña'),
                    ),
                    const SizedBox(height: 10),
                    TextButton.icon(
                      style: TextButton.styleFrom(foregroundColor: Colores.peligro),
                      onPressed: _cerrarSesion,
                      icon: const Icon(Icons.logout),
                      label: const Text('Cerrar sesión'),
                    ),
                  ],
                ),
    );
  }

  Widget _tarjetaUsuario() {
    final u = _usuario!;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: Colores.institucional, borderRadius: BorderRadius.circular(16)),
      child: Row(
        children: [
          CircleAvatar(
            radius: 28,
            backgroundColor: Colores.esmeralda,
            child: Text(
              u.nombre.isEmpty ? '?' : u.nombre[0].toUpperCase(),
              style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(u.nombre, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                Text(u.correo, style: const TextStyle(color: Colors.white70)),
                const SizedBox(height: 4),
                Text(
                  [
                    Formatos.tiposDonante[u.tipoDonante] ?? 'Donante',
                    if (u.identificacion != null) u.identificacion!,
                  ].join(' · '),
                  style: const TextStyle(color: Colors.white70, fontSize: 13),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
