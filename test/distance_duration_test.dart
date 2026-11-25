import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';
import 'package:sagay_tourist_navigator/services/distance_service.dart';

class _StubClient extends http.BaseClient {
  _StubClient(this.body);

  final String body;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async =>
      http.StreamedResponse(
        Stream<List<int>>.value(utf8.encode(body)),
        200,
      );
}

void main() {
  const from = LatLng(10.9, 123.41);
  const to = LatLng(10.95, 123.46);

  test('OSRM duration is parsed into the result', () async {
    final service = DistanceService(
      client: _StubClient(jsonEncode({
        'code': 'Ok',
        'routes': [
          {
            'distance': 7300.0,
            'duration': 1260.0,
            'geometry': {
              'coordinates': [
                [123.41, 10.9],
                [123.46, 10.95],
              ],
            },
          },
        ],
      })),
    );
    final result = await service.getDistanceKm(
      from: from,
      to: to,
      roadFactor: 1.3,
    );
    expect(result.isRoadRoute, isTrue);
    expect(result.km, closeTo(7.3, 0.001));
    expect(result.durationSeconds, 1260);
    expect(result.routePoints.length, 2);
  });

  test('offline fallback keeps an estimated travel time', () async {
    final service = DistanceService(client: _StubClient('not json'));
    final result = await service.getDistanceKm(
      from: from,
      to: to,
      roadFactor: 1.3,
    );
    expect(result.isRoadRoute, isFalse);
    expect(result.durationSeconds, isNotNull);
    expect(result.durationSeconds, greaterThan(0));
  });
}
