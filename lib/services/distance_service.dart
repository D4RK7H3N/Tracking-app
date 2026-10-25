import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';

/// The distance to a destination plus how it was obtained.
class DistanceResult {
  const DistanceResult({
    required this.km,
    required this.isRoadRoute,
    this.routePoints = const [],
  });

  /// Travel distance in kilometres.
  final double km;

  /// true  -> came from the OSRM road graph (real driving route).
  /// false -> straight-line distance x road_factor (offline estimate).
  /// The UI shows an "Estimated (offline)" label when this is false.
  final bool isRoadRoute;

  /// Waypoints of the actual road route (user -> destination), used to draw
  /// the route line on the map. Empty for the offline straight-line fallback.
  final List<LatLng> routePoints;
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
    final road = await _fetchRoadRoute(from, to);
    if (road != null && road.km > 0) {
      return DistanceResult(
        km: road.km,
        isRoadRoute: true,
        routePoints: road.points,
      );
    }
    return DistanceResult(
      km: haversineKm(from, to) * roadFactor,
      isRoadRoute: false,
    );
  }

  /// Fetches the driving route from the OSRM API, or null on any failure.
  Future<_RoadRoute?> _fetchRoadRoute(LatLng from, LatLng to) async {
    try {
      // OSRM expects "{lng},{lat};{lng},{lat}". overview=simplified returns a
      // light geometry we can draw as the route line on the map.
      final path =
          '${from.longitude},${from.latitude};${to.longitude},${to.latitude}';
      final uri =
          Uri.parse('$baseUrl/$path?overview=simplified&geometries=geojson');
      final response = await _client.get(uri).timeout(requestTimeout);
      if (response.statusCode != 200) {
        return null;
      }
      return _parseRoadRoute(response.body);
    } catch (_) {
      // Network error, timeout or malformed response -> caller falls back
      // to the offline estimate.
      return null;
    }
  }

  /// Extracts the distance (metres -> km) and the route geometry
  /// (`routes[0].geometry.coordinates`, [lng, lat] pairs) from an OSRM
  /// response body. Returns null when the response is unusable.
  _RoadRoute? _parseRoadRoute(String body) {
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
      if (metres == null) {
        return null;
      }

      final points = <LatLng>[];
      final geometry = first['geometry'];
      if (geometry is Map<String, dynamic>) {
        final coordinates = geometry['coordinates'];
        if (coordinates is List) {
          for (final c in coordinates) {
            if (c is List && c.length >= 2 && c[0] is num && c[1] is num) {
              points.add(LatLng((c[1] as num).toDouble(), (c[0] as num).toDouble()));
            }
          }
        }
      }
      return _RoadRoute(km: metres / 1000.0, points: points);
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

  /// Initial bearing (degrees, 0-360; 0 = due north, 90 = east) from [a]
  /// to [b]. Used to rotate the direction arrow on the user's marker.
  static double bearingDeg(LatLng a, LatLng b) {
    final double lat1 = _degToRad(a.latitude);
    final double lat2 = _degToRad(b.latitude);
    final double dLon = _degToRad(b.longitude - a.longitude);
    final double y = math.sin(dLon) * math.cos(lat2);
    final double x = math.cos(lat1) * math.sin(lat2) -
        math.sin(lat1) * math.cos(lat2) * math.cos(dLon);
    return (math.atan2(y, x) * 180.0 / math.pi + 360.0) % 360.0;
  }

  static double _degToRad(double degrees) => degrees * math.pi / 180.0;
}

/// Internal holder for one OSRM route: distance plus its polyline geometry.
class _RoadRoute {
  const _RoadRoute({required this.km, required this.points});

  final double km;
  final List<LatLng> points;
}
