import 'package:flutter/material.dart';

import '../config/tema.dart';
import '../models/donacion.dart';
import '../models/impacto.dart';
import '../services/auth_service.dart';
import '../services/donacion_service.dart';
import '../utils/formatos.dart';
import '../widgets/estado_chip.dart';
import '../widgets/mensajes.dart';
import '../widgets/tarjeta_indicador.dart';
import 'detalle_donacion_screen.dart';
import 'nueva_donacion_screen.dart';
import 'perfil_screen.dart';

/// Pantalla 2 - Inicio. Es el caso de uso principal: CU06 Consultar mis donaciones.
/// Muestra:
///  - Los indicadores de impacto del donante.
///  - La lista de sus donaciones, con filtro por estado (extends "Filtrar").
///  - El boton "Cargar más" (extends "Cargar más") y deslizar hacia abajo para actualizar (extends "Actualizar").
///  - El boton + para registrar una donacion (CU03).
///  - Al tocar una donacion se abre su detalle y seguimiento (CU04).
class InicioScreen extends StatefulWidget {
  const InicioScreen({super.key});

  @override
  State<InicioScreen> createState() => _InicioScreenState();
}

class _InicioScreenState extends State<InicioScreen> {
  final DonacionService _servicio = DonacionService();
  final AuthService _auth = AuthService();

  Impacto? _impacto;
  final List<Donacion> _donaciones = [];
  int _pagina = 0;
  bool _hayMas = false;
  String? _estadoFiltro; // null = todas

  bool _cargando = true;
  bool _cargandoMas = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargarTodo();
  }

  /// Carga los indicadores y la primera pagina de donaciones.
  Future<void> _cargarTodo() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final impacto = await _servicio.obtenerImpacto();
      final pagina = await _servicio.listar(pagina: 0, estado: _estadoFiltro);
      if (!mounted) return;
      setState(() {
        _impacto = impacto;
        _donaciones
          ..clear()
          ..addAll(pagina.contenido);
        _pagina = 0;
        _hayMas = pagina.hayMas;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  /// Extends "Cargar más": trae la siguiente pagina y la agrega al final.
  Future<void> _cargarMas() async {
    setState(() => _cargandoMas = true);
    try {
      final pagina = await _servicio.listar(pagina: _pagina + 1, estado: _estadoFiltro);
      if (!mounted) return;
      setState(() {
        _donaciones.addAll(pagina.contenido);
        _pagina = pagina.pagina;
        _hayMas = pagina.hayMas;
      });
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _cargandoMas = false);
    }
  }

  /// Extends "Filtrar por estado".
  void _cambiarFiltro(String? estado) {
    setState(() => _estadoFiltro = estado);
    _cargarTodo();
  }

  Future<void> _abrirNuevaDonacion() async {
    final creada = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => const NuevaDonacionScreen()),
    );
    if (creada == true) _cargarTodo();
  }

  Future<void> _abrirDetalle(Donacion d) async {
    final cambio = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => DetalleDonacionScreen(donacionId: d.id, codigo: d.codigo)),
    );
    if (cambio == true) _cargarTodo();
  }

  Future<void> _abrirPerfil() async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => const PerfilScreen()));
    if (mounted) setState(() {}); // por si cambio el nombre
  }

  @override
  Widget build(BuildContext context) {
    final nombre = _auth.usuarioActual?.primerNombre ?? '';
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis donaciones'),
        actions: [
          IconButton(
            tooltip: 'Mi perfil',
            icon: const Icon(Icons.account_circle_outlined),
            onPressed: _abrirPerfil,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _abrirNuevaDonacion,
        icon: const Icon(Icons.add),
        label: const Text('Donar'),
      ),
      body: _cargando
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? VistaError(mensaje: _error!, alReintentar: _cargarTodo)
              : RefreshIndicator(
                  // Extends "Actualizar": deslizar hacia abajo recarga todo
                  onRefresh: _cargarTodo,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                    children: [
                      Text('Hola, $nombre 👋',
                          style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colores.institucional)),
                      const SizedBox(height: 4),
                      const Text('Este es el impacto de tus donaciones.', style: TextStyle(color: Colores.textoSuave)),
                      if (_auth.usuarioActual?.debeCambiarPassword ?? false) _avisoClave(),
                      const SizedBox(height: 16),
                      _indicadores(),
                      const SizedBox(height: 20),
                      _filtros(),
                      const SizedBox(height: 12),
                      if (_donaciones.isEmpty) _vacio() else ..._donaciones.map(_tarjetaDonacion),
                      if (_hayMas)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: OutlinedButton(
                            onPressed: _cargandoMas ? null : _cargarMas,
                            child: _cargandoMas
                                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                                : const Text('Cargar más'),
                          ),
                        ),
                    ],
                  ),
                ),
    );
  }

  Widget _avisoClave() {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colores.advertencia.withOpacity(0.12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.warning_amber, color: Colores.advertencia),
          const SizedBox(width: 10),
          const Expanded(child: Text('Tu contraseña es temporal. Cámbiala desde tu perfil.')),
          TextButton(onPressed: _abrirPerfil, child: const Text('Ir')),
        ],
      ),
    );
  }

  Widget _indicadores() {
    final i = _impacto;
    if (i == null) return const SizedBox.shrink();
    return Column(
      children: [
        Row(children: [
          Expanded(
            child: TarjetaIndicador(
                icono: Icons.volunteer_activism, color: Colores.esmeralda, valor: '${i.totalDonaciones}', titulo: 'Donaciones'),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: TarjetaIndicador(
                icono: Icons.inventory_2_outlined, color: Colores.dinamico, valor: '${i.unidadesDonadas}', titulo: 'Unidades donadas'),
          ),
        ]),
        const SizedBox(height: 10),
        Row(children: [
          Expanded(
            child: TarjetaIndicador(
                icono: Icons.local_shipping_outlined,
                color: Colores.esmeraldaOscuro,
                valor: '${i.unidadesEntregadas}',
                titulo: 'Unidades entregadas'),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: TarjetaIndicador(
                icono: Icons.home_work_outlined,
                color: Colores.institucional,
                valor: '${i.fundacionesAtendidas.length}',
                titulo: 'Fundaciones beneficiadas'),
          ),
        ]),
      ],
    );
  }

  Widget _filtros() {
    return SizedBox(
      height: 40,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: const Text('Todas'),
              selected: _estadoFiltro == null,
              onSelected: (_) => _cambiarFiltro(null),
            ),
          ),
          ...Formatos.estados.entries.map((e) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(e.value),
                  selected: _estadoFiltro == e.key,
                  onSelected: (_) => _cambiarFiltro(e.key),
                ),
              )),
        ],
      ),
    );
  }

  Widget _vacio() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 40),
      child: Column(
        children: [
          const Icon(Icons.inbox_outlined, size: 56, color: Colores.textoSuave),
          const SizedBox(height: 10),
          Text(
            _estadoFiltro == null
                ? 'Aún no tienes donaciones. Toca "Donar" para registrar la primera.'
                : 'No tienes donaciones en este estado.',
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colores.textoSuave),
          ),
        ],
      ),
    );
  }

  Widget _tarjetaDonacion(Donacion d) {
    final productos = d.items.map((i) => i.nombreProducto).join(', ');
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: Colores.borde),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: () => _abrirDetalle(d),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(d.codigo,
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colores.institucional)),
                  ),
                  EstadoChip(estado: d.estado, titulo: d.estadoTitulo),
                ],
              ),
              const SizedBox(height: 6),
              Text(productos, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colores.texto)),
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(Icons.calendar_today, size: 14, color: Colores.textoSuave),
                  const SizedBox(width: 4),
                  Text(Formatos.fecha(d.fechaRegistro), style: const TextStyle(color: Colores.textoSuave, fontSize: 13)),
                  const Spacer(),
                  Text('${d.totalUnidades} unidades',
                      style: const TextStyle(color: Colores.textoSuave, fontSize: 13)),
                  const Icon(Icons.chevron_right, color: Colores.textoSuave),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
