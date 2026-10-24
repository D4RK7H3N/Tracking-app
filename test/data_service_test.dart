import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/models/app_data.dart';
import 'package:sagay_tourist_navigator/services/data_service.dart';

/// Guards against broken data edits: if this test fails after someone
/// replaces the JSON values, the data file (or its schema) is at fault.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late AppData data;

  setUpAll(() async {
    data = await DataService().load();
  });

  test('asset parses with the expected top-level sections', () {
    expect(data.version, 1);
    expect(data.terminals, isNotEmpty);
    expect(data.destinations, isNotEmpty);
  });

  test('every destination references a known terminal', () {
    for (final destination in data.destinations) {
      expect(
        data.terminalById(destination.terminalId),
        isNotNull,
        reason:
            'Destination "${destination.name}" references unknown terminal '
            '"${destination.terminalId}"',
      );
    }
  });

  test('terminal colours are valid #RRGGBB values', () {
    final hexPattern = RegExp(r'^#[0-9A-Fa-f]{6}$');
    for (final terminal in data.terminals) {
      expect(
        hexPattern.hasMatch(terminal.color),
        isTrue,
        reason:
            'Terminal "${terminal.name}" has an invalid colour '
            '"${terminal.color}" (expected #RRGGBB)',
      );
    }
  });

  test('fare rule values are positive', () {
    expect(data.fareRule.baseFare, greaterThan(0));
    expect(data.fareRule.baseDistanceKm, greaterThan(0));
    expect(data.fareRule.stepKm, greaterThan(0));
    expect(data.fareRule.extraPerStep, greaterThanOrEqualTo(0));
    expect(data.fareRule.roadFactor, greaterThanOrEqualTo(1));
  });
}
