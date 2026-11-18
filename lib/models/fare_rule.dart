import 'fare_matrix.dart';

class FareRule {
  const FareRule({
    required this.baseFare,
    required this.baseKm,
    required this.extraPerKm,
    required this.source,
    this.maxFare,
  });

  factory FareRule.fromJson(Map<String, dynamic> json) => FareRule(
        baseFare: (json['baseFare'] as num?)?.toDouble() ?? 15,
        baseKm: (json['baseKm'] as num?)?.toDouble() ?? 3,
        extraPerKm: (json['extraPerKm'] as num?)?.toDouble() ?? 2,
        maxFare: (json['maxFare'] as num?)?.toDouble(),
        source: json['source'] as String? ?? '',
      );

  final double baseFare;
  final double baseKm;
  final double extraPerKm;
  final double? maxFare;
  final String source;

  double compute(double roadKm) {
    final raw = roadKm <= baseKm
        ? baseFare
        : baseFare + (roadKm - baseKm).ceil() * extraPerKm;
    final cap = maxFare;
    if (cap == null) return raw;
    return raw.clamp(baseFare, cap).toDouble();
  }
}

class Discount {
  const Discount({required this.label, required this.percent});

  final String label;
  final double percent;

  static const matrix = Discount(label: 'Senior / Student / PWD', percent: 20);

  Map<String, dynamic> toJson() => {'label': label, 'percent': percent};
}

class FareEstimate {
  const FareEstimate({
    required this.amount,
    required this.official,
    required this.basis,
    required this.discounted,
  });

  final double amount;
  final bool official;
  final String basis;
  final bool discounted;
}

FareEstimate estimateFare({
  required FareRule rule,
  required FareMatrixLookup lookup,
  required String originZone,
  required String destinationZone,
  required double roadKm,
  required bool discounted,
  String? originName,
}) {
  final entry = lookup.find(originZone, destinationZone);
  if (entry != null) {
    return FareEstimate(
      amount: discounted ? entry.discounted : entry.regular,
      official: true,
      basis: 'Official fare matrix: ${originName ?? originZone} \u2192 $destinationZone',
      discounted: discounted,
    );
  }
  return FareEstimate(
    amount: rule.compute(roadKm),
    official: false,
    basis: 'Estimated from road distance (outside the matrix): '
        '\u20B1${rule.baseFare.toInt()} first ${rule.baseKm.toInt()} km, '
        '+\u20B1${rule.extraPerKm.toInt()} per started km',
    discounted: false,
  );
}
