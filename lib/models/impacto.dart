/// Indicadores de impacto del donante (viene de ImpactoDonanteResponse).
class Impacto {
  final int totalDonaciones;
  final int unidadesDonadas;
  final int unidadesEntregadas;
  final int unidadesEnBodega;
  final List<String> fundacionesAtendidas;
  final int certificadosDisponibles;

  Impacto({
    required this.totalDonaciones,
    required this.unidadesDonadas,
    required this.unidadesEntregadas,
    required this.unidadesEnBodega,
    required this.fundacionesAtendidas,
    required this.certificadosDisponibles,
  });

  factory Impacto.fromJson(Map<String, dynamic> json) {
    return Impacto(
      totalDonaciones: (json['totalDonaciones'] as num?)?.toInt() ?? 0,
      unidadesDonadas: (json['unidadesDonadas'] as num?)?.toInt() ?? 0,
      unidadesEntregadas: (json['unidadesEntregadas'] as num?)?.toInt() ?? 0,
      unidadesEnBodega: (json['unidadesEnBodega'] as num?)?.toInt() ?? 0,
      fundacionesAtendidas:
          (json['fundacionesAtendidas'] as List<dynamic>? ?? []).map((e) => e.toString()).toList(),
      certificadosDisponibles: (json['certificadosDisponibles'] as num?)?.toInt() ?? 0,
    );
  }
}
