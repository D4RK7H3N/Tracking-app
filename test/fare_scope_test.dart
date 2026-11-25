import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/app_data.dart';
import 'package:sagay_tourist_navigator/models/destination.dart';
import 'package:sagay_tourist_navigator/models/fare_scope.dart';
import 'package:sagay_tourist_navigator/services/data_service.dart';
import 'package:sagay_tourist_navigator/services/fare_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late AppData data;
  const service = FareService();

  setUpAll(() async {
    data = await DataService().load();
  });

  Destination find(String id) =>
      data.destinations.firstWhere((destination) => destination.id == id);

  FareQuote quoteFor(String id, {String originZone = 'CITY_PROPER'}) =>
      service.quote(
        data: data,
        destination: find(id),
        roadKm: 20,
        discounted: true,
        originZone: originZone,
      );

  test('official tricycle matrix fare keeps the 20% discount', () {
    final quote = quoteFor('D11'); // Sagay Public Mall, Aguilar Subdivision
    expect(quote.scope, isA<MotorizedTricycleFare>());
    expect(quote.scope.isOfficial, isTrue);
    expect(quote.scope.canDiscount, isTrue);
    expect(quote.scope.amount, 12);
    expect(quote.scope.label, 'official matrix fare');
    expect(quote.estimate.discounted, isTrue);
  });

  test('bus routes show no verified fare and no discount', () {
    final quote = quoteFor('D10'); // Bulanon: R-BULANON is a bus route
    expect(data.routeForDestination(find('D10'))?.vehicle, 'bus');
    expect(quote.scope, isA<BusFare>());
    expect(quote.scope.canDiscount, isFalse);
    expect(quote.scope.headline((value) => '₱$value'), 'Not verified');
    expect(quote.scope.basis, contains('Bus fare not verified'));
    // No unpublished discounted price leaks into the history estimate.
    expect(quote.estimate.discounted, isFalse);
  });

  test('island trips price only the tricycle leg to the port', () {
    final quote = quoteFor('D1', originZone: 'CITY_PROPER');
    expect(quote.scope, isA<IslandFare>());
    final island = quote.scope as IslandFare;
    expect(island.tricycleToPort.isOfficial, isTrue);
    expect(island.canDiscount, isTrue);
    expect(island.amount, 16); // CITY_PROPER -> BRGY. OLD SAGAY discounted
    expect(island.basis, contains('Boat fare not verified'));
  });

  test('unknown vehicles are unverified', () {
    const unknown = Destination(
      id: 'unknown',
      name: 'unknown',
      category: 'test',
      lat: 0,
      lng: 0,
      terminalId: 'T1',
      zone: '',
      routeId: 'DOES-NOT-EXIST',
    );
    final quote = service.quote(
      data: data,
      destination: unknown,
      roadKm: 5,
      discounted: true,
      originZone: 'CITY_PROPER',
    );
    expect(quote.scope, isA<UnverifiedFare>());
    expect(quote.scope.canDiscount, isFalse);
    expect(quote.scope.headline((value) => '₱$value'), 'Not verified');
  });

  test('estimate() still returns the original matrix math for parity', () {
    final estimate = service.estimate(
      data: data,
      destination: find('D11'),
      roadKm: 20,
      discounted: true,
      originZone: 'CITY_PROPER',
    );
    expect(estimate.official, isTrue);
    expect(estimate.amount, 12);
  });
}
