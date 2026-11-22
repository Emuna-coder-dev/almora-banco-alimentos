import 'package:flutter/material.dart';

import '../config/tema.dart';
import '../models/donacion.dart';
import '../models/producto.dart';
import '../services/donacion_service.dart';
import '../services/producto_service.dart';
import '../utils/formatos.dart';
import '../widgets/mensajes.dart';

/// Pantalla 3 - Registrar donacion (CU03).
/// Si recibe una [donacion], la pantalla sirve para EDITAR esa donacion pendiente.
class NuevaDonacionScreen extends StatefulWidget {
  final Donacion? donacion;

  const NuevaDonacionScreen({super.key, this.donacion});

  @override
  State<NuevaDonacionScreen> createState() => _NuevaDonacionScreenState();
}

class _NuevaDonacionScreenState extends State<NuevaDonacionScreen> {
  final ProductoService _productoService = ProductoService();
  final DonacionService _donacionService = DonacionService();
  final _observaciones = TextEditingController();

  List<Producto> _productos = [];
  final List<ItemFormulario> _items = [];

  bool _cargando = true;
  bool _guardando = false;
  String? _error;

  bool get _esEdicion => widget.donacion != null;

  @override
  void initState() {
    super.initState();
    _cargarProductos();
  }

  @override
  void dispose() {
    _observaciones.dispose();
    super.dispose();
  }

  Future<void> _cargarProductos() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final productos = await _productoService.listar();
      if (!mounted) return;
      setState(() {
        _productos = productos;
        _items.clear();
        final d = widget.donacion;
        if (d != null) {
          // Modo edicion: se llenan los campos con los datos actuales
          _observaciones.text = d.observaciones ?? '';
          for (final i in d.items) {
            _items.add(ItemFormulario(
              productoId: i.productoId,
              cantidad: i.cantidad,
              fechaVencimiento: i.fechaVencimiento == null ? null : DateTime.tryParse(i.fechaVencimiento!),
            ));
          }
        }
        if (_items.isEmpty) _items.add(ItemFormulario());
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  Producto? _buscarProducto(String? id) {
    for (final p in _productos) {
      if (p.id == id) return p;
    }
    return null;
  }

  /// Revisa el formulario antes de enviarlo. Devuelve el error o null si todo esta bien.
  String? _validar() {
    if (_items.isEmpty) return 'Agrega al menos un producto';
    final hoy = DateTime.now();
    final inicioHoy = DateTime(hoy.year, hoy.month, hoy.day);
    for (var n = 0; n < _items.length; n++) {
      final item = _items[n];
      final producto = _buscarProducto(item.productoId);
      if (producto == null) return 'Selecciona el producto de la fila ${n + 1}';
      if (item.cantidad < 1) return 'La cantidad de ${producto.nombre} debe ser mayor a 0';
      if (producto.perecedero && item.fechaVencimiento == null) {
        return '${producto.nombre} es perecedero: indica su fecha de vencimiento';
      }
      if (item.fechaVencimiento != null && item.fechaVencimiento!.isBefore(inicioHoy)) {
        return 'La fecha de vencimiento de ${producto.nombre} ya pasó';
      }
    }
    return null;
  }

  Future<void> _guardar() async {
    final error = _validar();
    if (error != null) {
      Mensajes.error(context, error);
      return;
    }
    setState(() => _guardando = true);
    try {
      final Donacion resultado = _esEdicion
          ? await _donacionService.editar(widget.donacion!.id, _items, _observaciones.text)
          : await _donacionService.registrar(_items, _observaciones.text);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (_) => AlertDialog(
          icon: const Icon(Icons.check_circle, color: Colores.esmeralda, size: 48),
          title: Text(_esEdicion ? 'Donación actualizada' : '¡Gracias por donar!'),
          content: Text(
            'Código de tu donación: ${resultado.codigo}\n\n'
            'Estado: ${resultado.estadoTitulo}. Lleva los productos al banco de alimentos; '
            'cuando los reciban podrás seguir su ruta desde la app.',
          ),
          actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Entendido'))],
        ),
      );
      if (mounted) Navigator.of(context).pop(true); // true = la pantalla anterior debe recargar
    } catch (e) {
      if (mounted) Mensajes.error(context, e);
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  Future<void> _elegirFecha(ItemFormulario item) async {
    final hoy = DateTime.now();
    final fecha = await showDatePicker(
      context: context,
      initialDate: item.fechaVencimiento ?? hoy.add(const Duration(days: 30)),
      firstDate: DateTime(hoy.year, hoy.month, hoy.day),
      lastDate: DateTime(hoy.year + 5),
      helpText: 'Fecha de vencimiento',
    );
    if (fecha != null) setState(() => item.fechaVencimiento = fecha);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_esEdicion ? 'Editar ${widget.donacion!.codigo}' : 'Nueva donación')),
      body: _cargando
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? VistaError(mensaje: _error!, alReintentar: _cargarProductos)
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    const Text('¿Qué vas a donar?',
                        style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colores.institucional)),
                    const SizedBox(height: 4),
                    const Text('Agrega cada producto con su cantidad. Los perecederos necesitan fecha de vencimiento.',
                        style: TextStyle(color: Colores.textoSuave)),
                    const SizedBox(height: 16),
                    for (var n = 0; n < _items.length; n++) _filaItem(n),
                    TextButton.icon(
                      onPressed: () => setState(() => _items.add(ItemFormulario())),
                      icon: const Icon(Icons.add),
                      label: const Text('Agregar otro producto'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _observaciones,
                      maxLines: 3,
                      maxLength: 500,
                      decoration: const InputDecoration(
                        labelText: 'Observaciones (opcional)',
                        hintText: 'Ej: los llevo el lunes en la mañana',
                      ),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _guardando ? null : _guardar,
                      icon: _guardando
                          ? const SizedBox(
                              width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Icon(Icons.send),
                      label: Text(_esEdicion ? 'Guardar cambios' : 'Registrar donación'),
                    ),
                  ],
                ),
    );
  }

  /// Una fila del formulario: producto + cantidad + fecha de vencimiento.
  Widget _filaItem(int n) {
    final item = _items[n];
    final producto = _buscarProducto(item.productoId);
    return Card(
      // ObjectKey: cada tarjeta queda "amarrada" a su item, asi al quitar una fila no se mezclan los datos
      key: ObjectKey(item),
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: Colores.borde),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Text('Producto ${n + 1}', style: const TextStyle(fontWeight: FontWeight.w600)),
                const Spacer(),
                if (_items.length > 1)
                  IconButton(
                    tooltip: 'Quitar',
                    icon: const Icon(Icons.delete_outline, color: Colores.peligro),
                    onPressed: () => setState(() => _items.removeAt(n)),
                  ),
              ],
            ),
            DropdownButtonFormField<String>(
              value: producto?.id,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Producto'),
              items: _productos
                  .map((p) => DropdownMenuItem(
                        value: p.id,
                        child: Text('${p.nombre} (${Formatos.unidad(p.unidadMedida)})', overflow: TextOverflow.ellipsis),
                      ))
                  .toList(),
              onChanged: (id) => setState(() => item.productoId = id),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    initialValue: '${item.cantidad}',
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      labelText: 'Cantidad',
                      suffixText: producto == null ? null : Formatos.unidad(producto.unidadMedida),
                    ),
                    onChanged: (v) => item.cantidad = int.tryParse(v) ?? 0,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: InkWell(
                    onTap: () => _elegirFecha(item),
                    child: InputDecorator(
                      decoration: InputDecoration(
                        labelText: (producto?.perecedero ?? false) ? 'Vence *' : 'Vence',
                        suffixIcon: const Icon(Icons.event),
                      ),
                      child: Text(item.fechaVencimiento == null ? '-' : Formatos.fechaCorta(item.fechaVencimiento!)),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
