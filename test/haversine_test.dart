import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:sagay_tourist_navigator/services/distance_service.dart';

void main() {
  group('DistanceService.haversineKm', () {
    test('same point gives zero distance', () {
      const point = LatLng(10.92, 123.45);
      expect(DistanceService.haversineKm(point, point), closeTo(0, 0.0001));
    });

    test('0.01° of latitude is about 1.11 km', () {
      const a = LatLng(10.90, 123.42);
      const b = LatLng(10.91, 123.42);
      expect(DistanceService.haversineKm(a, b), closeTo(1.11, 0.02));
    });

    test('one degree of longitude at the equator is about 111.2 km', () {
      const a = LatLng(0.0, 120.0);
      const b = LatLng(0.0, 121.0);
      expect(DistanceService.haversineKm(a, b), closeTo(111.19, 0.5));
    });

    test('diagonal Sagay example lands in a sane range', () {
      // 0.01° lat + 0.01° lng near Sagay should be roughly 1.5-1.6 km.
      const a = LatLng(10.90, 123.42);
      const b = LatLng(10.91, 123.43);
      final km = DistanceService.haversineKm(a, b);
      expect(km, greaterThan(1.4));
      expect(km, lessThan(1.7));
    });

    test('distance is symmetric', () {
      const a = LatLng(10.9451, 123.4799);
      const b = LatLng(10.8940, 123.4360);
      final ab = DistanceService.haversineKm(a, b);
      final ba = DistanceService.haversineKm(b, a);
      expect(ab, closeTo(ba, 0.000001));
    });
  });

  group('DistanceResult', () {
    test('road-route flag is preserved', () {
      const road = DistanceResult(km: 5.2, isRoadRoute: true);
      expect(road.km, 5.2);
      expect(road.isRoadRoute, isTrue);
    });

    test('offline fallback flag is preserved', () {
      const offline = DistanceResult(km: 3.2, isRoadRoute: false);
      expect(offline.km, 3.2);
      expect(offline.isRoadRoute, isFalse);
    });
  });
}
