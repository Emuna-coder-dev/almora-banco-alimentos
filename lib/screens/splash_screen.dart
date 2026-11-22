import 'package:flutter/material.dart';

import '../config/tema.dart';
import '../services/auth_service.dart';
import 'acceso_screen.dart';
import 'inicio_screen.dart';

/// Pantalla de carga: muestra el isotipo mientras revisa si el donante ya tenia la sesion iniciada.
/// - Si la sesion se puede recuperar -> va a Inicio (CU06).
/// - Si no -> va a Acceso (CU01 / CU02).
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  final AuthService _auth = AuthService();

  @override
  void initState() {
    super.initState();
    _revisarSesion();
  }

  Future<void> _revisarSesion() async {
    final haySesion = await _auth.recuperarSesion();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => haySesion ? const InicioScreen() : const AccesoScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colores.institucional,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(24)),
              child: Image.asset('assets/img/isotipo.png', width: 96, height: 96),
            ),
            const SizedBox(height: 20),
            const Text('Banco de Alimentos',
                style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            const Text('Compartir también es alimentar', style: TextStyle(color: Colors.white70)),
            const SizedBox(height: 28),
            const CircularProgressIndicator(color: Colores.esmeralda),
          ],
        ),
      ),
    );
  }
}
