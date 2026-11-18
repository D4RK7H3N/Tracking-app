import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/fare_matrix.dart';
import 'package:sagay_tourist_navigator/models/fare_rule.dart';
import 'package:sagay_tourist_navigator/services/fare_service.dart';

void main() {
  const rule = FareRule(
    baseFare: 15,
    baseKm: 3,
    extraPerKm: 2,
    maxFare: 40,
    source: 'Sagay City Resolution No. 2026-117',
  );
  const service = FareService();

  group('FareRule.compute', () {
    test('within the base distance charges only the base fare', () {
      expect(rule.compute(0), 15);
      expect(rule.compute(3), 15);
    });

    test('each started kilometre beyond the base adds the step', () {
      expect(rule.compute(3.01), 17);
      expect(rule.compute(8), 25);
    });

    test('the fare stays within the published range', () {
      expect(rule.compute(20), 40);
    });
  });

  group('FareMatrixLookup', () {
    final lookup = FareMatrixLookup(const [
      FareMatrixEntry(
        originZone: 'CITY_PROPER',
        destinationZone: 'BRGY. PARAISO/FABRICA',
        regular: 25,
        discounted: 20,
      ),
      FareMatrixEntry(
        originZone: 'OLD_SAGAY',
        destinationZone: 'SAGAY CITY',
        regular: 20,
        discounted: 16,
      ),
    ]);

    test('matches a short zone label against a compound matrix row', () {
      expect(lookup.find('CITY_PROPER', 'BRGY. FABRICA')?.regular, 25);
    });

    test('returns null for an unknown origin or blank destination', () {
      expect(lookup.find('NOWHERE', 'SAGAY CITY'), isNull);
      expect(lookup.find('OLD_SAGAY', ''), isNull);
    });
  });

  group('FareService', () {
    test('formatFare shows whole pesos or exact centavos', () {
      expect(service.formatFare(20), '\u20B120');
      expect(service.formatFare(13.6), '\u20B113.60');
    });

    test('group total multiplies the unit fare', () {
      expect(service.groupTotal(20, 3), 60);
      expect(service.groupTotal(16, 0), 16);
    });

    test('CSV escaping keeps commas and quotes intact', () {
      expect(service.csvRow(
        date: '2026-11-18',
        origin: 'Old Sagay',
        destination: 'The Mall, Sagay',
        vehicle: 'tricycle',
        fare: 20,
        discounted: false,
        passengers: 2,
        total: 40,
      ), contains('"The Mall, Sagay"'));
    });
  });
}
