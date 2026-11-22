import 'dart:io';

import 'package:flutter/material.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

import '../config/tema.dart';
import '../models/donacion.dart';
import '../models/seguimiento.dart';
import '../services/donacion_service.dart';
import '../utils/formatos.dart';
import '../widgets/estado_chip.dart';
import '../widgets/mensajes.dart';
import 'nueva_donacion_screen.dart';

/// Pantalla 4 - Detalle y seguimiento de una donacion.
///  - CU04 Rastrear donacion: ruta tipo guia, progreso, eventos y fundaciones destino.
///  - Extends "Descargar certificado" (cuando la donacion ya fue entregada).
///  - CU05 Anular donacion (solo si esta PENDIENTE).
///  - Editar donacion (solo si esta PENDIENTE).
class DetalleDonacionScreen extends StatefulWidget {
  final String donacionId;
  final String codigo;

  const DetalleDonacionScreen({super.key, required this.donacionId, required this.codigo});

  @override
  State<DetalleDonacionScreen> createState() => _DetalleDonacionScreenState();
}

class _DetalleDonacionScreenState extends State<DetalleDonacionScreen> {
  final DonacionService _servicio = DonacionService();

  Donacion? _donacion;
  Seguimiento? _seguimiento;
  bool _cargando = true;
  bool _trabajando = false; // anulando o descargando
  bool _huboCambios = false; // para avisarle a Inicio que recargue
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final donacion = await _servicio.obtener(widget.donacionId);
      final seguimiento = await _servicio.seguimiento(donacion.id);
      if (!mounted) return;
      setState(() {
        _donacion = donacion;
        _seguimiento = seguimiento;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  // ======================================================================= CU05 anular
  Future<void> _anular() async {
    final motivo = TextEditingController();
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (contexto) {
        final formulario = GlobalKey<FormState>();
        return AlertDialog(
          title: const Text('Anular donación'),
          content: Form(
            key: formulario,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('Esta acción no se puede deshacer. Cuéntanos por qué la anulas.'),
                const SizedBox(height: 12),
                TextFormField(
                  controller: motivo,
                  maxLines: 3,
                  maxLength: 300,
                  decoration: const InputDecoration(labelText: 'Motivo'),
                  validator: (v) =>
                      (v == null || v.trim().length < 5) ? 'Escribe al menos 5 caracteres' : null,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('Cancelar')),
            TextButton(
              style: TextButton.styleFrom(foregroundColor: Colores.peligro),
              onPressed: () {
                if (formulario.currentState!.validate()) Navigator.pop(contexto, true);
              },
              child: const Text('Anular'),
            ),
          ],
        );
      },
    );
    if (confirmado != true) return;

    setState(() => _trabajando = true);
    try {
      await _servicio.anular(widget.donacionId, motivo.text);
      _huboCambios = true;
      if (mounted) Mensajes.exito(context, 'Donación anulada');
      await _cargar();
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _trabajando = false);
    }
  }

  // ======================================================================= editar
  Future<void> _editar() async {
    final editada = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => NuevaDonacionScreen(donacion: _donacion)),
    );
    if (editada == true) {
      _huboCambios = true;
      _cargar();
    }
  }

  // ======================================================================= certificado
  Future<void> _descargarCertificado() async {
    setState(() => _trabajando = true);
    try {
      final bytes = await _servicio.descargarCertificado(widget.donacionId);
      // Se guarda el PDF en una carpeta temporal del celular y se abre con el visor de PDF
      final carpeta = await getTemporaryDirectory();
      final archivo = File('${carpeta.path}/certificado-${_donacion!.codigo}.pdf');
      await archivo.writeAsBytes(bytes);
      final resultado = await OpenFilex.open(archivo.path);
      if (resultado.type != ResultType.done && mounted) {
        Mensajes.error(context, 'No hay una app para abrir PDF en este celular.');
      }
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _trabajando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      // Al volver atras se le dice a Inicio si debe recargar
      canPop: false,
      onPopInvokedWithResult: (yaSalio, _) {
        if (!yaSalio) Navigator.of(context).pop(_huboCambios);
      },
      child: Scaffold(
        appBar: AppBar(title: Text(widget.codigo)),
        body: _cargando
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? VistaError(mensaje: _error!, alReintentar: _cargar)
                : RefreshIndicator(
                    onRefresh: _cargar,
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        _encabezado(),
                        const SizedBox(height: 12),
                        _acciones(),
                        _seccion('Productos', Icons.inventory_2_outlined, _productos()),
                        _seccion('Progreso', Icons.pie_chart_outline, _progreso()),
                        _seccion('Ruta de tu donación', Icons.route_outlined, _ruta()),
                        if (_seguimiento!.destinos.isNotEmpty)
                          _seccion('Fundaciones que la recibieron', Icons.home_work_outlined, _destinos()),
                        if (_seguimiento!.eventos.isNotEmpty)
                          _seccion('Historial', Icons.history, _eventos()),
                      ],
                    ),
                  ),
      ),
    );
  }

  Widget _encabezado() {
    final d = _donacion!;
    final s = _seguimiento!;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colores.institucional,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(d.codigo,
                    style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
              ),
              Container(
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20)),
                child: EstadoChip(estado: d.estado, titulo: d.estadoTitulo),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(s.estadoDescripcion, style: const TextStyle(color: Colors.white70)),
          const SizedBox(height: 8),
          Text('Registrada el ${Formatos.fecha(d.fechaRegistro)} · ${d.totalUnidades} unidades',
              style: const TextStyle(color: Colors.white, fontSize: 13)),
          if (d.observaciones != null && d.observaciones!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text('Nota: ${d.observaciones}', style: const TextStyle(color: Colors.white70, fontSize: 13)),
          ],
        ],
      ),
    );
  }

  Widget _acciones() {
    final d = _donacion!;
    final botones = <Widget>[];
    if (d.certificadoDisponible) {
      botones.add(ElevatedButton.icon(
        onPressed: _trabajando ? null : _descargarCertificado,
        icon: const Icon(Icons.workspace_premium_outlined),
        label: const Text('Descargar certificado'),
      ));
    }
    if (d.esPendiente) {
      botones.add(Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              onPressed: _trabajando ? null : _editar,
              icon: const Icon(Icons.edit_outlined),
              label: const Text('Editar'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: OutlinedButton.icon(
              style: OutlinedButton.styleFrom(foregroundColor: Colores.peligro),
              onPressed: _trabajando ? null : _anular,
              icon: const Icon(Icons.cancel_outlined),
              label: const Text('Anular'),
            ),
          ),
        ],
      ));
    }
    if (_trabajando) botones.add(const LinearProgressIndicator());
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final b in botones) Padding(padding: const EdgeInsets.only(bottom: 8), child: b),
      ],
    );
  }

  /// Tarjeta blanca con un titulo y contenido.
  Widget _seccion(String titulo, IconData icono, Widget contenido) {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colores.borde),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Icon(icono, color: Colores.institucional, size: 20),
            const SizedBox(width: 8),
            Text(titulo, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colores.institucional)),
          ]),
          const SizedBox(height: 10),
          contenido,
        ],
      ),
    );
  }

  Widget _productos() {
    return Column(
      children: _donacion!.items.map((i) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(i.nombreProducto, style: const TextStyle(fontWeight: FontWeight.w600)),
                    if (i.fechaVencimiento != null)
                      Text('Vence: ${Formatos.fecha(i.fechaVencimiento)}',
                          style: const TextStyle(color: Colores.textoSuave, fontSize: 12)),
                  ],
                ),
              ),
              Text('${i.cantidad} ${Formatos.unidad(i.unidadMedida)}', style: const TextStyle(fontWeight: FontWeight.w600)),
            ],
          ),
        );
      }).toList(),
    );
  }

  Widget _progreso() {
    final p = _seguimiento!.progreso;
    Widget dato(String titulo, int valor, Color color) => Column(
          children: [
            Text('$valor', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color)),
            Text(titulo, style: const TextStyle(fontSize: 11, color: Colores.textoSuave)),
          ],
        );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('${p.porcentajeEntregado}% entregado a fundaciones'),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: LinearProgressIndicator(
            value: p.porcentajeEntregado / 100,
            minHeight: 10,
            backgroundColor: Colores.borde,
            color: Colores.esmeralda,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            dato('En bodega', p.enBodega, Colores.dinamico),
            dato('Asignadas', p.asignadas, Colores.institucional),
            dato('En ruta', p.enRuta, Colores.institucional),
            dato('Entregadas', p.entregadas, Colores.esmeraldaOscuro),
          ],
        ),
        if (p.vencidas > 0 || p.anuladas > 0) ...[
          const SizedBox(height: 8),
          Text('Vencidas: ${p.vencidas} · Anuladas: ${p.anuladas}',
              style: const TextStyle(color: Colores.textoSuave, fontSize: 12)),
        ],
      ],
    );
  }

  /// Ruta tipo guia de envio: un punto por cada etapa, unidos por una linea.
  Widget _ruta() {
    final pasos = _seguimiento!.ruta;
    return Column(
      children: [
        for (var n = 0; n < pasos.length; n++)
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  width: 28,
                  child: Column(
                    children: [
                      Icon(
                        pasos[n].completado ? Icons.check_circle : (pasos[n].actual ? Icons.radio_button_checked : Icons.radio_button_unchecked),
                        color: pasos[n].completado || pasos[n].actual ? Colores.esmeralda : Colores.borde,
                        size: 22,
                      ),
                      if (n < pasos.length - 1)
                        Expanded(
                          child: Container(
                            width: 2,
                            color: pasos[n].completado ? Colores.esmeralda : Colores.borde,
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          pasos[n].titulo,
                          style: TextStyle(
                            fontWeight: pasos[n].actual ? FontWeight.bold : FontWeight.w500,
                            color: pasos[n].completado || pasos[n].actual ? Colores.texto : Colores.textoSuave,
                          ),
                        ),
                        if (pasos[n].fecha != null)
                          Text(Formatos.fechaHora(pasos[n].fecha),
                              style: const TextStyle(color: Colores.textoSuave, fontSize: 12)),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }

  Widget _destinos() {
    return Column(
      children: _seguimiento!.destinos.map((Destino d) {
        return ListTile(
          contentPadding: EdgeInsets.zero,
          leading: const CircleAvatar(
            backgroundColor: Color(0x1A10B981),
            child: Icon(Icons.favorite, color: Colores.esmeralda),
          ),
          title: Text(d.fundacion, style: const TextStyle(fontWeight: FontWeight.w600)),
          subtitle: Text(
            '${d.unidades} unidades · ${d.estadoTitulo}'
            '${d.fechaEntrega != null ? '\nEntregada el ${Formatos.fecha(d.fechaEntrega)}' : ''}'
            '${d.receptor != null ? ' · Recibió: ${d.receptor}' : ''}',
          ),
        );
      }).toList(),
    );
  }

  Widget _eventos() {
    // Del mas reciente al mas antiguo
    final eventos = _seguimiento!.eventos.reversed.toList();
    return Column(
      children: eventos.map((EventoSeguimiento e) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Icon(Icons.circle, size: 8, color: Colores.dinamico),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(e.titulo, style: const TextStyle(fontWeight: FontWeight.w600)),
                    if (e.descripcion != null && e.descripcion!.isNotEmpty) Text(e.descripcion!),
                    Text(
                      [Formatos.fechaHora(e.fecha), if (e.ubicacion != null) e.ubicacion!].join(' · '),
                      style: const TextStyle(color: Colores.textoSuave, fontSize: 12),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}
