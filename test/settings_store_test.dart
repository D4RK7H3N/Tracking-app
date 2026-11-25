import 'package:flutter_test/flutter_test.dart';
import 'package:sagay_tourist_navigator/services/settings_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const store = SettingsStore();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  test('reduce-motion override persists and can be cleared', () async {
    expect(await store.reduceMotion(), isNull);
    await store.setReduceMotion(true);
    expect(await store.reduceMotion(), isTrue);
    await store.setReduceMotion(null);
    expect(await store.reduceMotion(), isNull);
  });

  test('onboarding seen flag persists', () async {
    expect(await store.onboardingSeen(), isFalse);
    await store.markOnboardingSeen();
    expect(await store.onboardingSeen(), isTrue);
  });
}
