import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/fare_rule.dart';
import 'package:sagay_tourist_navigator/services/fare_service.dart';

void main() {
  // Mirrors the sample values in assets/data/sagay_data.json.
  const rule = FareRule(
    baseFare: 15,
    baseDistanceKm: 2,
    stepKm: 1.5,
    extraPerStep: 10,
    roadFactor: 1.3,
  );
  const fareService = FareService();

  group('FareRule.fareFor (stepped formula)', () {
    test('within the base distance charges only the base fare', () {
      expect(rule.fareFor(0), 15);
      expect(rule.fareFor(1.0), 15);
      expect(rule.fareFor(1.99), 15);
    });

    test('exactly the base distance charges only the base fare', () {
      expect(rule.fareFor(2.0), 15);
    });

    test('just above the base distance adds exactly one step', () {
      // (2.01 - 2) / 1.5 = 0.0067 -> ceil = 1 step
      expect(rule.fareFor(2.01), 25);
    });

    test('multiple steps are counted correctly', () {
      // (6.4 - 2) / 1.5 = 2.93 -> ceil = 3 steps
      expect(rule.fareFor(6.4), 15 + 3 * 10);
    });

    test('an exact step boundary counts that step once', () {
      // (5.0 - 2) / 1.5 = 2.0 exactly -> 2 steps
      expect(rule.fareFor(5.0), 15 + 2 * 10);
    });

    test('just below a step boundary still counts only whole steps', () {
      // (4.9 - 2) / 1.5 = 1.93 -> ceil = 2 steps
      expect(rule.fareFor(4.9), 35);
    });
  });

  group('FareService.computeFare (discounts)', () {
    test('no discount returns the plain fare', () {
      expect(fareService.computeFare(rule: rule, distanceKm: 6.4), 45);
    });

    test('a discount is applied as a percentage of the computed fare', () {
      const discount = Discount(label: 'Senior / PWD', percent: 20);
      // 45 * 0.80 = 36
      expect(
        fareService.computeFare(
          rule: rule,
          distanceKm: 6.4,
          discount: discount,
        ),
        36,
      );
    });

    test('formatFare rounds to a whole peso', () {
      expect(fareService.formatFare(36.4), '₱36');
      expect(fareService.formatFare(36.5), '₱37');
    });
  });

  group('FareRule JSON parsing', () {
    test('parses all fields including discounts', () {
      final parsed = FareRule.fromJson(const {
        'base_fare': 15,
        'base_distance_km': 2,
        'step_km': 1.5,
        'extra_per_step': 10,
        'road_factor': 1.3,
        'discounts': [
          {'label': 'Senior / PWD', 'percent': 20},
        ],
      });
      expect(parsed.baseFare, 15);
      expect(parsed.baseDistanceKm, 2);
      expect(parsed.stepKm, 1.5);
      expect(parsed.extraPerStep, 10);
      expect(parsed.roadFactor, 1.3);
      expect(parsed.discounts.length, 1);
      expect(parsed.firstDiscount?.label, 'Senior / PWD');
      expect(parsed.firstDiscount?.percent, 20);
    });

    test('an empty discounts list means no toggle on the result screen', () {
      const parsed = FareRule(
        baseFare: 15,
        baseDistanceKm: 2,
        stepKm: 1.5,
        extraPerStep: 10,
        roadFactor: 1.3,
      );
      expect(parsed.firstDiscount, isNull);
    });
  });
}
