import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/app_data.dart';
import 'package:sagay_tourist_navigator/services/data_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late AppData data;

  setUpAll(() async {
    data = await DataService().load();
  });

  test('the bundled asset is the shared v3 contract', () {
    expect(data.version, 3);
    expect(data.terminals.length, 3);
    expect(data.destinations.length, 38);
    expect(data.fareMatrix.length, 40);
    expect(data.routes.length, 7);
    expect(data.routeAccess.length, 1);
  });

  test('every destination resolves to a terminal and a route', () {
    for (final destination in data.destinations) {
      expect(data.terminalById(destination.terminalId), isNotNull,
          reason: '${destination.name} has an unknown terminal');
      expect(data.routeById(destination.routeId), isNotNull,
          reason: '${destination.name} has an unknown route');
    }
  });

  test('every route references a known terminal', () {
    for (final route in data.routes) {
      expect(data.terminalById(route.terminalId), isNotNull,
          reason: '${route.name} has an unknown terminal');
    }
  });

  test('colours use the #RRGGBB form', () {
    final pattern = RegExp(r'^#[0-9A-Fa-f]{6}$');
    for (final route in data.routes) {
      expect(pattern.hasMatch(route.colorHex), isTrue,
          reason: '${route.name} has an invalid colour');
    }
    for (final terminal in data.terminals) {
      expect(pattern.hasMatch(terminal.color), isTrue,
          reason: '${terminal.name} has an invalid colour');
    }
  });

  test('the Old Sagay corridor rule resolves two services', () {
    final destination = data.destinations.firstWhere((d) => d.id == 'D6');
    final journey = data.journeyFor('OLD_SAGAY', destination);
    expect(journey.services.map((route) => route.id),
        ['R-OLD-SAGAY', 'R-POPLACION']);
    expect(journey.changeAt, 'Sagay City Bus Terminal (Poblacion)');
    expect(journey.fareZone, 'SAGAY CITY');
  });
}
