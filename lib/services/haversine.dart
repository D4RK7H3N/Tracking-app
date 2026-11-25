import 'dart:math';

double haversineKm(double lat1, double lng1, double lat2, double lng2) {
  const radius = 6371.0;
  final dLat = _radians(lat2 - lat1);
  final dLng = _radians(lng2 - lng1);
  final a = sin(dLat / 2) * sin(dLat / 2) +
      cos(_radians(lat1)) * cos(_radians(lat2)) * sin(dLng / 2) * sin(dLng / 2);
  return radius * 2 * atan2(sqrt(a), sqrt(1 - a));
}

/// Sanity check only — never used to change the fare.
///
/// A road route should be at least the straight-line distance and at most
/// three times it. Mirrors the native app's `Haversine.isRoadDistanceSuspicious`.
bool isRoadDistanceSuspicious(double roadKm, double straightLineKm) =>
    straightLineKm > 0 &&
    (roadKm < straightLineKm || roadKm > straightLineKm * 3);

double _radians(double degrees) => degrees * pi / 180;
