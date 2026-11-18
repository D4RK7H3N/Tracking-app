import '../services/haversine.dart';

class FareOrigin {
  const FareOrigin({
    required this.zone,
    required this.name,
    required this.lat,
    required this.lng,
    required this.radiusKm,
  });

  factory FareOrigin.fromJson(Map<String, dynamic> json) => FareOrigin(
        zone: json['zone'] as String? ?? '',
        name: json['name'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        radiusKm: (json['radiusKm'] as num?)?.toDouble() ?? 0,
      );

  final String zone;
  final String name;
  final double lat;
  final double lng;
  final double radiusKm;
}

FareOrigin? detectFareOrigin(double lat, double lng, List<FareOrigin> origins) {
  FareOrigin? best;
  var bestDistance = double.maxFinite;
  for (final origin in origins) {
    final distance = haversineKm(lat, lng, origin.lat, origin.lng);
    if (distance <= origin.radiusKm && distance < bestDistance) {
      best = origin;
      bestDistance = distance;
    }
  }
  return best;
}
