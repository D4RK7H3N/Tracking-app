/// A single fare discount entry, e.g. "Senior Citizen / PWD - 20%".
///
/// Parsed from the `fare_rule.discounts` array in
/// `assets/data/sagay_data.json`. The result screen offers the FIRST entry
/// in this list as an optional toggle.
class Discount {
  const Discount({required this.label, required this.percent});

  factory Discount.fromJson(Map<String, dynamic> json) => Discount(
        label: json['label'] as String? ?? '',
        percent: (json['percent'] as num?)?.toDouble() ?? 0,
      );

  final String label;

  /// Percentage taken off the computed fare (0-100).
  final double percent;

  Map<String, dynamic> toJson() => {'label': label, 'percent': percent};
}

/// Contract for computing a tricycle fare from a road distance in km.
///
/// The current implementation ([FareRule]) is the simple stepped formula
/// described in the app requirements. If the city later publishes a
/// lookup table (per-zone or per-route fares) instead of a formula,
/// implement that behind this same interface and swap it in
/// [AppData.fromJson] — no UI or service code needs to change.
abstract class FarePolicy {
  /// Returns the fare (in Philippine pesos) for the given road distance
  /// in kilometres.
  double fareFor(double distanceKm);
}

/// JSON-backed fare rule implementing the stepped fare formula:
///
/// * `distanceKm <= baseDistanceKm`                        -> `baseFare`
/// * otherwise:
///   `baseFare + ceil((distanceKm - baseDistanceKm) / stepKm) * extraPerStep`
class FareRule implements FarePolicy {
  const FareRule({
    required this.baseFare,
    required this.baseDistanceKm,
    required this.stepKm,
    required this.extraPerStep,
    required this.roadFactor,
    this.discounts = const [],
  });

  factory FareRule.fromJson(Map<String, dynamic> json) => FareRule(
        baseFare: (json['base_fare'] as num?)?.toDouble() ?? 0,
        baseDistanceKm: (json['base_distance_km'] as num?)?.toDouble() ?? 0,
        stepKm: (json['step_km'] as num?)?.toDouble() ?? 1,
        extraPerStep: (json['extra_per_step'] as num?)?.toDouble() ?? 0,
        roadFactor: (json['road_factor'] as num?)?.toDouble() ?? 1.25,
        discounts: (json['discounts'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Discount.fromJson)
            .toList(),
      );

  /// Fare charged for any trip up to [baseDistanceKm].
  final double baseFare;

  /// Distance (km) covered by [baseFare].
  final double baseDistanceKm;

  /// Each full [stepKm] beyond [baseDistanceKm] adds [extraPerStep]
  /// (partial steps are rounded UP with ceil).
  final double stepKm;
  final double extraPerStep;

  /// Multiplier applied to the straight-line distance when no road route is
  /// available (offline fallback). See README "Calibrating road_factor".
  final double roadFactor;

  final List<Discount> discounts;

  /// The first discount entry, or null when the list is empty.
  /// The result screen shows this as the optional discount toggle.
  Discount? get firstDiscount => discounts.isEmpty ? null : discounts.first;

  @override
  double fareFor(double distanceKm) {
    if (distanceKm <= baseDistanceKm) {
      return baseFare;
    }
    final steps = ((distanceKm - baseDistanceKm) / stepKm).ceil();
    return baseFare + steps * extraPerStep;
  }

  Map<String, dynamic> toJson() => {
        'base_fare': baseFare,
        'base_distance_km': baseDistanceKm,
        'step_km': stepKm,
        'extra_per_step': extraPerStep,
        'road_factor': roadFactor,
        'discounts': discounts.map((d) => d.toJson()).toList(),
      };
}
