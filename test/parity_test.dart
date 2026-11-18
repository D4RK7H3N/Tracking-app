import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/app_data.dart';
import 'package:sagay_tourist_navigator/models/destination.dart';
import 'package:sagay_tourist_navigator/models/fare_origin.dart';
import 'package:sagay_tourist_navigator/services/data_service.dart';
import 'package:sagay_tourist_navigator/services/fare_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late AppData data;
  final golden = jsonDecode(
        File('shared/sagay-contract/cases/golden.json').readAsStringSync(),
      ) as Map<String, dynamic>;
  const service = FareService();

  setUpAll(() async {
    data = await DataService().load();
  });

  group('shared golden fare cases', () {
    for (final raw in golden['fare'] as List<dynamic>) {
      final entry = raw as Map<String, dynamic>;
      test(entry['id'] as String, () {
        final result = service.estimate(
          data: data,
          destination: Destination(
            id: 'case',
            name: 'case',
            category: 'case',
            lat: 0,
            lng: 0,
            terminalId: 'T1',
            zone: entry['destinationZone'] as String? ?? '',
            routeId: 'R-POPLACION',
          ),
          roadKm: (entry['roadKm'] as num).toDouble(),
          discounted: entry['discounted'] as bool,
          originZone: entry['originZone'] as String,
        );
        expect(result.amount, closeTo((entry['expectedAmount'] as num).toDouble(), 0.001));
        expect(result.official, entry['expectedOfficial'] as bool);
      });
    }
  });

  group('shared golden zone cases', () {
    for (final raw in golden['zones'] as List<dynamic>) {
      final entry = raw as Map<String, dynamic>;
      test(entry['id'] as String, () {
        final origin = detectFareOrigin(
          (entry['lat'] as num).toDouble(),
          (entry['lng'] as num).toDouble(),
          data.fareOrigins,
        );
        expect(origin?.zone, entry['expectedZone']);
        expect(origin?.name, entry['expectedName']);
      });
    }
  });

  group('shared golden journey cases', () {
    for (final raw in golden['journeys'] as List<dynamic>) {
      final entry = raw as Map<String, dynamic>;
      test(entry['id'] as String, () {
        final destination = Destination(
          id: entry['id'] as String,
          name: entry['id'] as String,
          category: 'case',
          lat: 0,
          lng: 0,
          terminalId: 'T1',
          zone: entry['destinationZone'] as String,
          routeId: entry['destinationRouteId'] as String,
        );
        final journey = data.journeyFor(entry['originZone'] as String, destination);
        expect(journey.services.map((route) => route.id).toList(),
            (entry['expectedServices'] as List<dynamic>).cast<String>());
        expect(journey.changeAt, entry['expectedChangeAt']);
        expect(journey.fareZone, entry['expectedFareZone']);

        final roadKm = (entry['roadKm'] as num).toDouble();
        final regular = service.estimate(
          data: data,
          destination: destination,
          roadKm: roadKm,
          discounted: false,
          originZone: entry['originZone'] as String,
        );
        final discounted = service.estimate(
          data: data,
          destination: destination,
          roadKm: roadKm,
          discounted: true,
          originZone: entry['originZone'] as String,
        );
        expect(regular.amount, (entry['expectedRegular'] as num).toDouble());
        expect(discounted.amount, (entry['expectedDiscounted'] as num).toDouble());
      });
    }
  });

  group('shared golden group cases', () {
    for (final raw in golden['groups'] as List<dynamic>) {
      final entry = raw as Map<String, dynamic>;
      test(entry['id'] as String, () {
        expect(
          service.groupTotal(
            (entry['unitAmount'] as num).toDouble(),
            entry['passengers'] as int,
          ),
          (entry['expectedTotal'] as num).toDouble(),
        );
      });
    }
  });
}
