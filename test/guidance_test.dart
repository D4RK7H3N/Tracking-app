import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/services/guidance.dart';

void main() {
  final points = <({double lat, double lng})>[
    (lat: 10.900, lng: 123.410),
    (lat: 10.910, lng: 123.420),
    (lat: 10.920, lng: 123.430),
    (lat: 10.930, lng: 123.440),
  ];

  test('starting point prompts the change', () {
    final snapshot = computeGuidance(
      points: points,
      currentLat: 10.900,
      currentLng: 123.410,
      destinationName: 'The Mall',
      changeAtName: 'Sagay City Bus Terminal (Poblacion)',
      changeAtLat: 10.910,
      changeAtLng: 123.420,
    );
    expect(snapshot.progress, closeTo(0, 0.01));
    expect(snapshot.remainingKm, greaterThan(3));
    expect(snapshot.nextPrompt, 'Change at Sagay City Bus Terminal (Poblacion)');
    expect(snapshot.arrived, isFalse);
  });

  test('past the change point prompts the destination', () {
    final snapshot = computeGuidance(
      points: points,
      currentLat: 10.925,
      currentLng: 123.435,
      destinationName: 'The Mall',
      changeAtName: 'Sagay City Bus Terminal (Poblacion)',
      changeAtLat: 10.910,
      changeAtLng: 123.420,
    );
    expect(snapshot.progress, greaterThan(0.6));
    expect(snapshot.nextPrompt, 'Continue to The Mall');
  });

  test('the final point reports arrival', () {
    final snapshot = computeGuidance(
      points: points,
      currentLat: 10.930,
      currentLng: 123.440,
      destinationName: 'The Mall',
    );
    expect(snapshot.arrived, isTrue);
    expect(snapshot.nextPrompt, 'You have arrived at The Mall');
  });
}
