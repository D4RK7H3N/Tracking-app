import '../models/app_data.dart';
import '../models/destination.dart';
import '../models/fare_origin.dart';
import '../models/fare_rule.dart';
import '../models/fare_scope.dart';

class FareService {
  const FareService();

  FareEstimate estimate({
    required AppData data,
    required Destination destination,
    required double roadKm,
    required bool discounted,
    double? originLat,
    double? originLng,
    String? originZone,
  }) {
    final resolved = _resolve(
      data: data,
      destination: destination,
      originLat: originLat,
      originLng: originLng,
      originZone: originZone,
    );
    return _price(
      data: data,
      resolved: resolved,
      roadKm: roadKm,
      discounted: discounted,
    );
  }

  /// Prices the trip and explains what the amount covers, scoped to the
  /// vehicle that carries it. The fare math is identical to [estimate]; only
  /// the interpretation of the amount changes (bus/unverified show no fare,
  /// island fares price the tricycle leg to the port).
  FareQuote quote({
    required AppData data,
    required Destination destination,
    required double roadKm,
    required bool discounted,
    double? originLat,
    double? originLng,
    String? originZone,
  }) {
    final resolved = _resolve(
      data: data,
      destination: destination,
      originLat: originLat,
      originLng: originLng,
      originZone: originZone,
    );
    final route = data.routeForDestination(destination);
    FareEstimate price(bool withDiscount) => _price(
          data: data,
          resolved: resolved,
          roadKm: roadKm,
          discounted: withDiscount,
        );
    FareScope scopeFor(FareEstimate estimate) => scopeForFare(
          estimate: estimate,
          vehicle: route?.vehicle,
          requiresBoat: destination.boat,
        );

    // The published 20% discount exists only for an official matrix fare.
    // Work out the regular price first so the scope is known before deciding
    // whether a discounted price may be offered at all.
    final regular = price(false);
    final regularScope = scopeFor(regular);
    if (!discounted || !regularScope.canDiscount) {
      return FareQuote(estimate: regular, scope: regularScope);
    }
    final discountedEstimate = price(true);
    return FareQuote(
      estimate: discountedEstimate,
      scope: scopeFor(discountedEstimate),
    );
  }

  _ResolvedFare _resolve({
    required AppData data,
    required Destination destination,
    double? originLat,
    double? originLng,
    String? originZone,
  }) {
    FareOrigin? origin;
    if (originZone == null && originLat != null && originLng != null) {
      origin = detectFareOrigin(originLat, originLng, data.fareOrigins);
    } else if (originZone != null) {
      for (final candidate in data.fareOrigins) {
        if (candidate.zone.toUpperCase() == originZone.toUpperCase()) {
          origin = candidate;
          break;
        }
      }
    }
    final zone = origin?.zone ?? originZone ?? '';
    final journey = data.journeyFor(zone, destination);
    return _ResolvedFare(
      zone: zone,
      destinationZone: journey.fareZone ?? destination.zone,
      originName: origin?.name,
    );
  }

  FareEstimate _price({
    required AppData data,
    required _ResolvedFare resolved,
    required double roadKm,
    required bool discounted,
  }) =>
      estimateFare(
        rule: data.fareRule,
        lookup: data.matrixLookup,
        originZone: resolved.zone,
        destinationZone: resolved.destinationZone,
        roadKm: roadKm,
        discounted: discounted,
        originName: resolved.originName,
      );

  String formatFare(double amount) {
    if (amount == amount.roundToDouble()) {
      return '\u20B1${amount.round()}';
    }
    return '\u20B1${amount.toStringAsFixed(2)}';
  }

  double groupTotal(double unitAmount, int passengers) =>
      unitAmount * (passengers < 1 ? 1 : passengers);

  String csvHeader() => 'date,origin,destination,vehicle,fare,discounted,passengers,total';

  String csvRow({
    required String date,
    required String origin,
    required String destination,
    required String vehicle,
    required double fare,
    required bool discounted,
    required int passengers,
    required double total,
  }) {
    return <String>[
      date,
      origin,
      destination,
      vehicle,
      fare.toStringAsFixed(2),
      discounted ? 'true' : 'false',
      passengers.toString(),
      total.toStringAsFixed(2),
    ].map(_escapeCsv).join(',');
  }

  String _escapeCsv(String value) {
    if (!value.contains(',') && !value.contains('"') && !value.contains('\n')) {
      return value;
    }
    return '"${value.replaceAll('"', '""')}"';
  }
}

class _ResolvedFare {
  const _ResolvedFare({
    required this.zone,
    required this.destinationZone,
    required this.originName,
  });

  final String zone;
  final String destinationZone;
  final String? originName;
}
