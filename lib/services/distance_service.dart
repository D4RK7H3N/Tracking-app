import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';

/// The distance to a destination plus how it was obtained.
class DistanceResult {
  const DistanceResult({required this.km, required this.isRoadRoute});

  /// Travel distance in kilometres.
  final double km;

  /// true  -> came from the OSRM road graph (real driving route).
  /// false -> straight-line distance x road_factor (offline estimate).
  /// The UI shows an "Estimated (offline)" label when this is false.
  final bool isRoadRoute;
}

/// Computes the travel distance between two points.
///
/// Primary source: the OSRM public routing API (no API key required).
/// Fallback: Haversine straight-line distance scaled by `road_factor`
/// from the JSON data — used when offline, on timeout, or when the API
/// returns no route.
///
/// To swap providers, change [defaultBaseUrl] (e.g. to an OpenRouteService
/// endpoint) and adjust [_parseRoadDistanceKm] to match the response shape.
class DistanceService {
  DistanceService({http.Client? client, String? baseUrl})
      : _client = client ?? http.Client(),
        baseUrl = baseUrl ?? defaultBaseUrl;

  /// Single constant so the routing provider is easy to swap.
  static const String defaultBaseUrl =
      'https://router.project-osrm.org/route/v1/driving';

  /// Road-distance request timeout.
  static const Duration requestTimeout = Duration(seconds: 6);

  final http.Client _client;
  final String baseUrl;

  /// Returns the distance between [from] and [to].
  ///
  /// Tries the road route first; on any failure falls back to
  /// `haversineKm(from, to) * roadFactor`.
  Future<DistanceResult> getDistanceKm({
    required LatLng from,
    required LatLng to,
    required double roadFactor,
  }) async {
    final roadKm = await _fetchRoadDistanceKm(from, to);
    if (roadKm != null && roadKm > 0) {
      return DistanceResult(km: roadKm, isRoadRoute: true);
    }
    return DistanceResult(
      km: haversineKm(from, to) * roadFactor,
      isRoadRoute: false,
    );
  }

  /// Fetches the driving distance from the OSRM API, or null on any failure.
  Future<double?> _fetchRoadDistanceKm(LatLng from, LatLng to) async {
    try {
      // OSRM expects "{lng},{lat};{lng},{lat}".
      final path =
          '${from.longitude},${from.latitude};${to.longitude},${to.latitude}';
      final uri = Uri.parse('$baseUrl/$path?overview=false');
      final response = await _client.get(uri).timeout(requestTimeout);
      if (response.statusCode != 200) {
        return null;
      }
      return _parseRoadDistanceKm(response.body);
    } catch (_) {
      // Network error, timeout or malformed response -> caller falls back
      // to the offline estimate.
      return null;
    }
  }

  /// Extracts `routes[0].distance` (metres) from an OSRM response body and
  /// converts it to kilometres. Returns null when the response is unusable.
  double? _parseRoadDistanceKm(String body) {
    try {
      final dynamic data = jsonDecode(body);
      if (data is! Map<String, dynamic>) {
        return null;
      }
      if (data['code'] != 'Ok') {
        return null;
      }
      final routes = data['routes'];
      if (routes is! List || routes.isEmpty) {
        return null;
      }
      final first = routes.first;
      if (first is! Map<String, dynamic>) {
        return null;
      }
      final metres = (first['distance'] as num?)?.toDouble();
      return metres == null ? null : metres / 1000.0;
    } catch (_) {
      return null;
    }
  }

  /// Great-circle (straight-line) distance between two points, in km.
  ///
  /// Kept as a pure static function so it can be unit tested directly.
  static double haversineKm(LatLng a, LatLng b) {
    const double earthRadiusKm = 6371.0;
    final double dLat = _degToRad(b.latitude - a.latitude);
    final double dLng = _degToRad(b.longitude - a.longitude);
    final double sinLat = math.sin(dLat / 2);
    final double sinLng = math.sin(dLng / 2);
    final double h = sinLat * sinLat +
        math.cos(_degToRad(a.latitude)) *
            math.cos(_degToRad(b.latitude)) *
            sinLng *
            sinLng;
    return 2 * earthRadiusKm * math.asin(math.sqrt(h));
  }

  static double _degToRad(double degrees) => degrees * math.pi / 180.0;
}
