import '../models/app_data.dart';
import '../models/destination.dart';
import '../models/fare_origin.dart';
import '../models/fare_rule.dart';

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
    return estimateFare(
      rule: data.fareRule,
      lookup: data.matrixLookup,
      originZone: zone,
      destinationZone: journey.fareZone ?? destination.zone,
      roadKm: roadKm,
      discounted: discounted,
      originName: origin?.name,
    );
  }

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
