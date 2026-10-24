import '../models/fare_rule.dart';

/// Turns a distance + fare rule (+ optional discount) into a displayed fare.
///
/// Kept separate from the UI so fare logic stays unit-testable, and so a
/// future lookup-table policy can replace [FareRule.fareFor] without any
/// screen changes.
class FareService {
  const FareService();

  /// Fare in Philippine pesos for [distanceKm] under [rule].
  ///
  /// When [discount] is given, its percent is taken off the computed fare.
  double computeFare({
    required FareRule rule,
    required double distanceKm,
    Discount? discount,
  }) {
    final baseFare = rule.fareFor(distanceKm);
    if (discount == null || discount.percent <= 0) {
      return baseFare;
    }
    return baseFare * (1 - discount.percent / 100);
  }

  /// Whole-peso display string, e.g. "₱35".
  String formatFare(double amount) => '₱${amount.round()}';
}
