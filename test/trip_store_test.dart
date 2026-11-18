import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/services/trip_store.dart';

void main() {
  test('trip records survive a JSON round trip', () {
    final record = TripRecord(
      id: '1',
      destinationId: 'D6',
      destinationName: 'Sagay Public Plaza',
      originZone: 'OLD_SAGAY',
      vehicleLabel: 'Yellow Tricycle',
      fare: 20,
      discounted: true,
      passengers: 3,
      total: 48,
      savedAt: DateTime.parse('2026-11-18T10:30:00.000'),
    );
    final restored = TripRecord.fromJson(record.toJson());
    expect(restored.destinationId, 'D6');
    expect(restored.fare, 20);
    expect(restored.discounted, isTrue);
    expect(restored.passengers, 3);
    expect(restored.total, 48);
    expect(restored.savedAt, record.savedAt);
  });
}
