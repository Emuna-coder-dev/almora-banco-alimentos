import 'package:flutter_test/flutter_test.dart';

import 'package:almora_movil/utils/formatos.dart';

/// Pruebas unitarias sencillas. Se ejecutan con: flutter test
void main() {
  test('Convierte una fecha del backend a dd/MM/yyyy', () {
    expect(Formatos.fecha('2026-12-31'), '31/12/2026');
  });

  test('Si no hay fecha muestra un guion', () {
    expect(Formatos.fecha(null), '-');
  });

  test('Muestra la unidad de medida corta', () {
    expect(Formatos.unidad('KILOGRAMO'), 'kg');
    expect(Formatos.unidad('UNIDAD'), 'und');
  });

  test('Tiene los 6 tipos de donante del backend', () {
    expect(Formatos.tiposDonante.length, 6);
  });
}
